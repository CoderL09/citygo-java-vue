package com.citygo.booking.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CheckoutRequest(@NotNull @Positive Long contactId,
        @NotEmpty @Size(max=10) List<@NotNull @Valid Line> items,
        @Size(max=255) String remark) {
    public record Line(@NotBlank @Pattern(regexp="SESSION|PACKAGE") String itemType,
            @NotNull @Positive Long itemId,@NotNull @Min(1) @Max(5) Integer quantity) {}
}
