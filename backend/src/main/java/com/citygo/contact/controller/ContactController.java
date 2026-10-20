package com.citygo.contact.controller;
import com.citygo.contact.service.ContactService;
import com.citygo.contact.entity.Contact;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/contacts") @RequiredArgsConstructor
public class ContactController {
    private final ContactService service;
    @GetMapping public ApiResponse<List<Contact>> list(@AuthenticationPrincipal LoginUser u){return ApiResponse.ok(service.list(u.id()));}
    @PostMapping public ApiResponse<Contact> create(@AuthenticationPrincipal LoginUser u,@Valid @RequestBody ContactService.Request r){return ApiResponse.ok(service.save(u.id(),null,r));}
    @PutMapping("/{id}") public ApiResponse<Contact> update(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@Valid @RequestBody ContactService.Request r){return ApiResponse.ok(service.save(u.id(),id,r));}
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){service.delete(u.id(),id);return ApiResponse.ok();}
}
