
package userone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="booking", url="http://booking:8080")
public interface BookingService {

    @RequestMapping(method= RequestMethod.PATCH, path="/bookings")
    public void bookingChange(@RequestBody Booking booking);

}