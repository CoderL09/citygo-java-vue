package com.citygo.catalog.service;

import com.citygo.catalog.dto.ActivityCreateRequest;
import com.citygo.catalog.dto.ActivityDetailView;
import com.citygo.catalog.dto.ActivityImageView;
import com.citygo.catalog.dto.ActivityStatusRequest;
import com.citygo.catalog.dto.ActivityUpdateRequest;
import com.citygo.catalog.dto.ActivityView;
import com.citygo.catalog.dto.ImageRefRequest;
import com.citygo.catalog.dto.SessionView;
import com.citygo.catalog.dto.SortOrderRequest;
import com.citygo.catalog.entity.Activity;
import com.citygo.catalog.entity.ActivityImage;
import com.citygo.catalog.entity.Category;
import com.citygo.catalog.entity.Session;
import com.citygo.catalog.mapper.ActivityImageMapper;
import com.citygo.catalog.mapper.ActivityMapper;
import com.citygo.catalog.mapper.CategoryMapper;
import com.citygo.catalog.mapper.SessionMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.api.PageResult;
import com.citygo.common.exception.BizException;
import com.citygo.file.entity.FileObject;
import com.citygo.file.mapper.FileObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    public static final String ON_SALE = "ON_SALE";

    private final ActivityMapper activityMapper;
    private final ActivityImageMapper activityImageMapper;
    private final SessionMapper sessionMapper;
    private final CategoryMapper categoryMapper;
    private final FileObjectMapper fileObjectMapper;
    private final Clock clock;

    /** 公开列表：仅上架活动 */
    public PageResult<ActivityView> pagePublic(Long categoryId, String keyword, int page, int pageSize) {
        return page(categoryId, ON_SALE, keyword, page, pageSize);
    }

    public PageResult<ActivityView> pageAdmin(Long categoryId, String status, String keyword,
                                              int page, int pageSize) {
        return page(categoryId, status, keyword, page, pageSize);
    }

    private PageResult<ActivityView> page(Long categoryId, String status, String keyword,
                                          int page, int pageSize) {
        long total = activityMapper.count(categoryId, status, keyword);
        List<ActivityView> items = activityMapper.page(categoryId, status, keyword,
                        (page - 1) * pageSize, pageSize).stream()
                .map(ActivityView::from)
                .toList();
        return PageResult.of(items, total, page, pageSize);
    }

    /** 公开详情：活动 + 相册 + 在售场次 */
    public ActivityDetailView detailPublic(Long id) {
        Activity activity = requireExists(id);
        if (!ON_SALE.equals(activity.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return toDetail(activity, ON_SALE);
    }

    /** 管理详情不受上下架影响，公开接口继续隐藏下架商品。 */
    public ActivityDetailView detailAdmin(Long id) {
        return toDetail(requireExists(id), null);
    }

    private ActivityDetailView toDetail(Activity activity, String sessionStatus) {
        List<ActivityImageView> images = activityImageMapper.findByActivityId(activity.getId()).stream()
                .map(i -> new ActivityImageView(i.getId(), i.getActivityId(), i.getFileObjectId(),
                        "/api/files/" + i.getStorageKey(), i.getSortOrder()))
                .toList();
        List<SessionView> sessions = sessionMapper.findByActivityId(activity.getId(), sessionStatus).stream()
                .map(SessionView::from)
                .toList();
        return new ActivityDetailView(activity.getId(), activity.getCategoryId(), activity.getCategoryName(),
                activity.getName(), activity.getDescription(), activity.getAddress(), activity.getStatus(),
                images, sessions);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ActivityView create(Long operatorId, ActivityCreateRequest request) {
        requireCategoryEnabled(request.categoryId());
        LocalDateTime now = LocalDateTime.now(clock);
        Activity activity = new Activity();
        activity.setCategoryId(request.categoryId());
        activity.setName(request.name());
        activity.setDescription(request.description());
        activity.setAddress(request.address());
        activity.setStatus(ON_SALE);
        activity.setCreatedBy(operatorId);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        activityMapper.insert(activity);
        return ActivityView.from(activityMapper.findById(activity.getId()));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ActivityView update(Long id, ActivityUpdateRequest request) {
        requireCategoryEnabled(request.categoryId());
        requireLocked(id);
        activityMapper.update(id, request.categoryId(), request.name(), request.description(),
                request.address(), LocalDateTime.now(clock));
        return ActivityView.from(activityMapper.findById(id));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ActivityView setStatus(Long id, ActivityStatusRequest request) {
        requireLocked(id);
        activityMapper.updateStatus(id, request.status(), LocalDateTime.now(clock));
        return ActivityView.from(activityMapper.findById(id));
    }

    /**
     * 删除：任一在场次被有效订单引用即拒绝（实施书 §6）；否则级联删除场次与图片引用。
     * 文件对象物理清理由 P5 孤立文件任务处理。
     */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(Long id) {
        requireLocked(id);
        List<Session> sessions = sessionMapper.findByActivityId(id, null).stream()
                .sorted(java.util.Comparator.comparing(Session::getId)).toList();
        sessions.forEach(s -> sessionMapper.lockById(s.getId()));
        for (Session s : sessions) {
            if (sessionMapper.countValidOrderRefs(s.getId()) > 0) {
                throw new BizException(ErrorCode.ACTIVITY_HAS_VALID_ORDERS);
            }
        }
        for (Session s : sessions) {
            if (sessionMapper.countAnyRefs(s.getId()) > 0) throw new BizException(ErrorCode.CONFLICT,"场次被套餐或历史订单引用，请下架代替删除");
        }
        sessions.forEach(s -> sessionMapper.deleteById(s.getId()));
        activityImageMapper.deleteByActivityId(id);
        activityMapper.deleteById(id);
    }

    /** 挂接图片：校验文件存在并标记 USED，防止把无关文件挂到活动上 */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ActivityImageView addImage(com.citygo.auth.security.LoginUser operator, Long activityId, ImageRefRequest request) {
        requireLocked(activityId);
        FileObject file = fileObjectMapper.lockById(request.fileObjectId());
        if (file == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        if (!"ADMIN".equals(operator.role()) && !operator.id().equals(file.getOwnerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能引用其他员工上传的文件");
        }
        if("DELETING".equals(file.getStatus()))throw new BizException(ErrorCode.CONFLICT,"文件正在删除");
        ActivityImage image = new ActivityImage();
        image.setActivityId(activityId);
        image.setFileObjectId(request.fileObjectId());
        image.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        image.setCreatedAt(LocalDateTime.now(clock));
        activityImageMapper.insert(image);
        fileObjectMapper.updateStatus(file.getId(), "USED");
        return new ActivityImageView(image.getId(), activityId, request.fileObjectId(),
                "/api/files/" + file.getStorageKey(), image.getSortOrder());
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void updateImageSort(Long imageId, SortOrderRequest request) {
        if (activityImageMapper.findById(imageId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        activityImageMapper.updateSort(imageId, request.sortOrder());
    }

    /** 移除图片：解除引用，文件回 TEMP 交由 P5 清理 */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void removeImage(Long imageId) {
        ActivityImage image = activityImageMapper.findById(imageId);
        if (image == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        requireLocked(image.getActivityId());
        fileObjectMapper.lockById(image.getFileObjectId());
        activityImageMapper.deleteById(imageId);
        if (fileObjectMapper.countImageRefs(image.getFileObjectId()) == 0) {
            fileObjectMapper.updateStatus(image.getFileObjectId(), "TEMP");
        }
    }

    private Activity requireLocked(Long id) {
        Activity activity=activityMapper.lockById(id);
        if(activity==null)throw new BizException(ErrorCode.NOT_FOUND);
        return activity;
    }

    private Activity requireExists(Long id) {
        Activity activity = activityMapper.findById(id);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    private void requireCategoryEnabled(Long categoryId) {
        Category category = categoryMapper.shareById(categoryId);
        if (category == null || !"ACTIVITY".equals(category.getCategoryType()) || !Boolean.TRUE.equals(category.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类不存在或已停用");
        }
    }
}
