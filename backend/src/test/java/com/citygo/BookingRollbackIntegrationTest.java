package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.cart.mapper.CartMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class BookingRollbackIntegrationTest extends P2Fixtures {
    @Autowired BookingService booking;
    @MockitoSpyBean CartMapper cart;
    @Test void failureAfterStockItemsAllocationsEventAndOutboxRollsBackEverything(){
        long uid=insertUser("fault","secret123","USER",true),c=contact(uid),s=session(5,1000);
        jdbcTemplate.update("INSERT INTO cart_items(user_id,item_type,item_id,quantity,selected,created_at,updated_at) VALUES(?,'SESSION',?,1,1,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))",uid,s);
        doThrow(new IllegalStateException("injected after all order writes")).when(cart).consume(anyLong(),anyString(),anyLong(),anyInt());
        assertThatThrownBy(()->booking.create(uid,"rollback-all-writes",request(c,line("SESSION",s,1)))).isInstanceOf(IllegalStateException.class);
        assertThat(stock(s)).isEqualTo(5);assertThat(count("cart_items")).isEqualTo(1);
        for(String table:java.util.List.of("orders","order_items","order_allocations","order_events","outbox_events"))assertThat(count(table)).as(table).isZero();
    }
}
