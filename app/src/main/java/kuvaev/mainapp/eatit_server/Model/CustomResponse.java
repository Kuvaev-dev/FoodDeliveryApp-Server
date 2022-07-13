package kuvaev.mainapp.eatit_server.Model;

import java.util.List;

import javax.xml.transform.Result;

public class CustomResponse {
    public long multicast_id;
    public int success;
    public int failure;
    public int canonical_ids;
    public List<Result> results;
}
