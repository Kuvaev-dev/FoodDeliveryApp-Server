package kuvaev.mainapp.eatit_server.Model;

public class Banner {
    private String id , name , image;

    public Banner() { }

    public Banner(String id, String name, String image) {
        this.id = id;  // this id is Category Id
        this.name = name;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
