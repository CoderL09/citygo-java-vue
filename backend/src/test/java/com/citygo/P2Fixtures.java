package com.citygo;
import com.citygo.booking.dto.CheckoutRequest;
import com.citygo.booking.dto.CheckoutRequest.Line;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.*;
abstract class P2Fixtures extends BaseIntegrationTest {
    @Autowired protected Clock clock;
    protected long category(String type){jdbcTemplate.update("INSERT INTO categories(category_type,name,sort_order,status,created_at,updated_at) VALUES(?,?,0,1,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))",type,"分类"+UUID.randomUUID());return jdbcTemplate.queryForObject("SELECT MAX(id) FROM categories",Long.class);}
    protected long activity(){long c=category("ACTIVITY");jdbcTemplate.update("INSERT INTO activities(category_id,name,address,status,created_at,updated_at) VALUES(?,'城市步行','集合地址','ON_SALE',UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))",c);return jdbcTemplate.queryForObject("SELECT MAX(id) FROM activities",Long.class);}
    protected long session(int capacity,int price){long a=activity();LocalDateTime now=LocalDateTime.now(clock);jdbcTemplate.update("INSERT INTO sessions(activity_id,start_time,end_time,booking_deadline,price_cents,capacity,available,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,'ON_SALE',?,?)",a,now.plusDays(10),now.plusDays(10).plusHours(2),now.plusDays(9),price,capacity,capacity,now,now);return jdbcTemplate.queryForObject("SELECT MAX(id) FROM sessions",Long.class);}
    protected long pkg(int price,long... ids){long c=category("PACKAGE");jdbcTemplate.update("INSERT INTO packages(category_id,name,package_price_cents,version,status,created_at,updated_at) VALUES(?,'博物馆+步行',?,1,'ON_SALE',UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))",c,price);long id=jdbcTemplate.queryForObject("SELECT MAX(id) FROM packages",Long.class);jdbcTemplate.update("UPDATE packages SET family_id=id WHERE id=?",id);for(long s:ids)jdbcTemplate.update("INSERT INTO package_items(package_id,session_id,sort_order,created_at) VALUES(?,?,0,UTC_TIMESTAMP(3))",id,s);return id;}
    protected long contact(long uid){jdbcTemplate.update("INSERT INTO contacts(user_id,name,phone,is_default,created_at,updated_at) VALUES(?,'张同学','13800000000',1,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))",uid);return jdbcTemplate.queryForObject("SELECT MAX(id) FROM contacts",Long.class);}
    protected CheckoutRequest request(long contact,Line... lines){return new CheckoutRequest(contact,List.of(lines),"");}
    protected Line line(String type,long id,int qty){return new Line(type,id,qty);}
    protected int stock(long id){return jdbcTemplate.queryForObject("SELECT available FROM sessions WHERE id=?",Integer.class,id);}
    protected int count(String table){return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
}
