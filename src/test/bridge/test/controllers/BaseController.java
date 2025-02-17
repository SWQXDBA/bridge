package bridge.test.controllers;

import io.github.swqxdba.bridge.meta.ApiComment;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

public class BaseController {

    /**
     * super注释
     */
    @RequestMapping("super")
    public String superTest() {
        return "super";
    }

    @ApiComment("777")
    @RequestMapping("super2")
    public String superTest2() {
        return "super2";
    }

    @RequestMapping("testEnum")
    public List<DemoEnum> testEnum(){
        return new ArrayList<>();
    }
}
