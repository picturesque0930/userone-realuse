package userone;

public class RoomDeleted extends AbstractEvent {

    private Long id;
    private String usable;
    private String name;

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