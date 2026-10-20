package com.citygo.cart.service;
import com.citygo.cart.entity.CartItem;
import com.citygo.cart.mapper.CartMapper;
import com.citygo.catalog.service.ProductQuoteService;
import com.citygo.booking.dto.CheckoutRequest.Line;
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
public class CartService {
    private final CartMapper mapper;private final UserLockMapper users;private final ProductQuoteService quotes;private final Clock clock;
    public record Add(@NotBlank @Pattern(regexp="SESSION|PACKAGE") String itemType,@NotNull @Positive Long itemId,@NotNull @Min(1) @Max(5) Integer quantity) {}
    public record Update(@NotNull @Min(1) @Max(5) Integer quantity,@NotNull Boolean selected) {}
    public record Row(CartItem item,ProductQuoteService.Quote product,boolean valid,String invalidReason) {}
    public List<Row> list(Long uid){return mapper.list(uid).stream().map(c->{try{
        return new Row(c,quotes.resolve(new Line(c.getItemType(),c.getItemId(),c.getQuantity())),true,null);
    }catch(BizException e){return new Row(c,null,false,e.getMessage());}}).toList();}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void add(Long uid,Add r){
        users.lock(uid);addLocked(uid,r);
    }
    public void addLocked(Long uid,Add r){
        CartItem c=mapper.product(uid,r.itemType(),r.itemId());int qty=r.quantity()+(c==null?0:c.getQuantity());
        if(qty>5)throw new BizException(ErrorCode.BAD_REQUEST,"同一商品最多 5 份");
        quotes.resolve(new Line(r.itemType(),r.itemId(),qty));
        LocalDateTime now=LocalDateTime.now(clock);
        if(c==null){if(mapper.list(uid).size()>=100)throw new BizException(ErrorCode.BAD_REQUEST,"购物车最多 100 行");
            c=new CartItem();c.setUserId(uid);c.setItemType(r.itemType());c.setItemId(r.itemId());c.setQuantity(qty);c.setSelected(true);c.setCreatedAt(now);c.setUpdatedAt(now);mapper.insert(c);
        }else{c.setQuantity(qty);c.setSelected(true);c.setUpdatedAt(now);mapper.update(c);}
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void update(Long uid,Long id,Update r){
        users.lock(uid);CartItem c=required(uid,id);
        // Invalid lines can still be deselected or removed; checkout always resolves prices again.
        if(!r.quantity().equals(c.getQuantity()))quotes.resolve(new Line(c.getItemType(),c.getItemId(),r.quantity()));
        c.setQuantity(r.quantity());c.setSelected(r.selected());c.setUpdatedAt(LocalDateTime.now(clock));mapper.update(c);
    }
    @Transactional public void delete(Long uid,Long id){users.lock(uid);required(uid,id);mapper.delete(uid,id);}
    @Transactional public void clear(Long uid){users.lock(uid);mapper.clear(uid);}
    private CartItem required(Long uid,Long id){CartItem c=mapper.find(uid,id);if(c==null)throw new BizException(ErrorCode.NOT_FOUND);return c;}
}
