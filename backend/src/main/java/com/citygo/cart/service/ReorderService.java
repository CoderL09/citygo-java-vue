package com.citygo.cart.service;
import com.citygo.booking.mapper.BookingMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import com.citygo.common.db.UserLockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
@Service @RequiredArgsConstructor public class ReorderService {
 private final BookingMapper orders;private final CartService cart;private final UserLockMapper users;
 public record Skipped(String itemType,Long itemId,String reason){}
 public record Result(int added,List<Skipped> skipped){}
 @Transactional(isolation=Isolation.READ_COMMITTED) public Result reorder(Long uid,Long orderId){users.lock(uid);if(orders.own(uid,orderId)==null)throw new BizException(ErrorCode.NOT_FOUND);int added=0;List<Skipped> skipped=new ArrayList<>();for(var i:orders.items(orderId)){try{cart.addLocked(uid,new CartService.Add(i.getItemType(),i.getItemId(),i.getQuantity()));added++;}catch(BizException e){skipped.add(new Skipped(i.getItemType(),i.getItemId(),e.getMessage()));}}return new Result(added,skipped);}
}
