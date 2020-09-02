
package userone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@FeignClient(name="booking", url="${api.url.booking}")
public interface BookingService {
    // 사용종료하면 Booking Cancel 을 이용하여 삭제한다.
    @DeleteMapping(value = "/bookings/{id}")
    public void bookingCancel(@PathVariable long id);

}
//
//
//
//@FeignClient(name="booking", url="${api.url.booking}")
//public interface BookingService {
//
//    // Booking Cancel 을 위한 삭제 mapping
//    @DeleteMapping(value = "/bookings/{id}")
//    public void bookingCancel(@PathVariable long id);