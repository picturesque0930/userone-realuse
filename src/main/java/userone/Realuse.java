package userone;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Realuse_table")
public class Realuse {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long bookingId;
    private String realUseStartDtm;
    private String realUseEndDtm;

    @PostPersist
    public void onPostPersist(){

        UseStarted useStarted = new UseStarted();
        BeanUtils.copyProperties(this, useStarted);
        useStarted.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        UseEnded useEnded = new UseEnded();
        BeanUtils.copyProperties(this, useEnded);
        useEnded.publishAfterCommit();
        System.out.println("#$#$#$#$#$#$#$#$ CANCEL[" + this.getBookingId() + "]");
        RealuseApplication.applicationContext.getBean(userone.external.BookingService.class).bookingCancel(this.getBookingId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getRealUseStartDtm() {
        return realUseStartDtm;
    }

    public void setRealUseStartDtm(String realUseStartDtm) {
        this.realUseStartDtm = realUseStartDtm;
    }

    public String getRealUseEndDtm() {
        return realUseEndDtm;
    }

    public void setRealUseEndDtm(String realUseEndDtm) {
        this.realUseEndDtm = realUseEndDtm;
    }
}
