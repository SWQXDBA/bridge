package bridge.test.controllers;

import io.github.swqxdba.bridge.meta.ApiComment;
import org.springframework.web.bind.annotation.RequestMapping;

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

}
