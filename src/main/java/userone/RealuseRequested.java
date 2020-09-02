package userone;

public class RealuseRequested extends AbstractEvent {

    private Long id;
    private Long bookingId;
    private String realUseStartDtm;
    private String realUseEndDtm;

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