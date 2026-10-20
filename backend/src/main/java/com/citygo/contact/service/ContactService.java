package com.citygo.contact.service;
import com.citygo.contact.entity.Contact;
import com.citygo.contact.mapper.ContactMapper;
import com.citygo.common.db.UserLockMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.List;
@Service @RequiredArgsConstructor
public class ContactService {
    private final ContactMapper mapper;private final UserLockMapper users;private final Clock clock;
    public record Request(@NotBlank @Size(max=64) String name,
        @NotBlank @Pattern(regexp="[0-9+() -]{5,20}") String phone,@NotNull Boolean defaultContact) {}
    public List<Contact> list(Long uid){return mapper.list(uid);}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Contact save(Long uid,Long id,Request r){
        users.lock(uid);List<Contact> all=mapper.list(uid);
        Contact c=id==null?new Contact():required(uid,id);
        if(id==null && all.size()>=50)throw new BizException(ErrorCode.BAD_REQUEST,"最多保存 50 位联系人");
        // Turning off the only default keeps it default; replacing/deleting it elects exactly one successor.
        boolean isDefault=r.defaultContact() || all.isEmpty() || Boolean.TRUE.equals(c.getDefaultContact());
        if(isDefault)mapper.clearDefault(uid);
        c.setUserId(uid);c.setName(r.name());c.setPhone(r.phone());c.setDefaultContact(isDefault);c.setUpdatedAt(LocalDateTime.now(clock));
        if(id==null){c.setCreatedAt(c.getUpdatedAt());mapper.insert(c);}else mapper.update(c);
        return required(uid,c.getId());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void delete(Long uid,Long id){
        users.lock(uid);Contact c=required(uid,id);mapper.delete(uid,id);
        if(Boolean.TRUE.equals(c.getDefaultContact())){List<Contact> rest=mapper.list(uid);if(!rest.isEmpty()){
            Contact next=rest.getFirst();next.setDefaultContact(true);next.setUpdatedAt(LocalDateTime.now(clock));mapper.update(next);
        }}
    }
    public Contact required(Long uid,Long id){Contact c=mapper.find(uid,id);if(c==null)throw new BizException(ErrorCode.NOT_FOUND,"联系人不存在");return c;}
}
