package bridge.test.controllers;

import io.github.swqxdba.bridge.meta.ApiComment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @RequestMapping("testRequestBody")
    public List<DemoEnum> testRequestBody(@RequestBody String a ){
        return new ArrayList<>();
    }

    @RequestMapping("testRequestBody")
    public List<DemoEnum> testRequestBody(@RequestBody Integer a ){
        return new ArrayList<>();
    }

//    @GetMapping("gettestRequestBody")
//    public List<DemoEnum> gettestRequestBody(@RequestBody String a ){
//        return new ArrayList<>();
//    }
}
