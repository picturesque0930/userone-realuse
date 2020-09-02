package userone;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Room_table")
public class Room {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String usable;
    private String name;

    @PostPersist
    public void onPostPersist(){
        RoomCreated roomCreated = new RoomCreated();
        BeanUtils.copyProperties(this, roomCreated);
        roomCreated.publishAfterCommit();


    }

    @PostUpdate
    public void onPostUpdate(){
        RoomChanged roomChanged = new RoomChanged();
        BeanUtils.copyProperties(this, roomChanged);
        roomChanged.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        userone.external.Booking booking = new userone.external.Booking();
        // mappings goes here
        RoomApplication.applicationContext.getBean(userone.external.BookingService.class)
            .bookingChange(booking);


    }

    @PreRemove
    public void onPreRemove(){
        RoomDeleted roomDeleted = new RoomDeleted();
        BeanUtils.copyProperties(this, roomDeleted);
        roomDeleted.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        userone.external.Booking booking = new userone.external.Booking();
        // mappings goes here
        RoomApplication.applicationContext.getBean(userone.external.BookingService.class)
            .bookingCancel(booking);


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getUsable() {
        return usable;
    }

    public void setUsable(String usable) {
        this.usable = usable;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }




}
