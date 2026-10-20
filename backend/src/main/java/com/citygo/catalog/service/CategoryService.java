package com.citygo.catalog.service;

import com.citygo.catalog.dto.CategoryCreateRequest;
import com.citygo.catalog.dto.CategoryStatusRequest;
import com.citygo.catalog.dto.CategoryUpdateRequest;
import com.citygo.catalog.dto.CategoryView;
import com.citygo.catalog.entity.Category;
import com.citygo.catalog.mapper.CategoryMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.api.PageResult;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final Clock clock;

    /** 公开列表：仅启用分类 */
    public PageResult<CategoryView> pagePublic(String categoryType, int page, int pageSize) {
        return page(categoryType, true, null, page, pageSize);
    }

    /** 管理列表：含停用，可按状态/关键字筛选 */
    public PageResult<CategoryView> pageAdmin(String categoryType, Boolean status, String keyword,
                                              int page, int pageSize) {
        return page(categoryType, status, keyword, page, pageSize);
    }

    private PageResult<CategoryView> page(String categoryType, Boolean status, String keyword,
                                          int page, int pageSize) {
        long total = categoryMapper.count(categoryType, status, keyword);
        List<CategoryView> items = categoryMapper.page(categoryType, status, keyword,
                        (page - 1) * pageSize, pageSize).stream()
                .map(CategoryView::from)
                .toList();
        return PageResult.of(items, total, page, pageSize);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CategoryView create(CategoryCreateRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Category category = new Category();
        category.setCategoryType(request.categoryType());
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(true);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
        return CategoryView.from(category);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CategoryView update(Long id, CategoryUpdateRequest request) {
        requireExists(id);
        categoryMapper.update(id, request.name(), request.sortOrder(), LocalDateTime.now(clock));
        return CategoryView.from(categoryMapper.findById(id));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CategoryView setStatus(Long id, CategoryStatusRequest request) {
        requireExists(id);
        categoryMapper.updateStatus(id, request.status(), LocalDateTime.now(clock));
        return CategoryView.from(categoryMapper.findById(id));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(Long id) {
        requireExists(id);
        // 关联限制（实施书 §2）：分类下存在活动时不允许删除
        if (categoryMapper.countActivities(id) > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        categoryMapper.deleteById(id);
    }

    private void requireExists(Long id) {
        if (categoryMapper.lockById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
    }
}
