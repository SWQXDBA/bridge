package bridge.test.controllers;

import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//测试controller
@RestController
@RequestMapping("test")
public class MyController extends BaseController{

    @GetMapping("enumKey")
    Map<DemoEnum,String> enumKey(){
        return new HashMap<>();
    }



    /// 666
    @GetMapping("users")
    List<Long> getUser( List<Long> idList){
        return null;
    }

    @PostMapping("users/{id}")
    Long[] path(@RequestBody List<Long> idList, @PathVariable("id") Long orId){
        return null;
    }

    @GetMapping("getUser4")
    Map<Long,Comparable<Comparator>> getUser4(List<Long> idList){

        return null;
    }
    <T>  List<T> getUser3(){

        return null;
    }
    @GetMapping("users")
    List<Long> getUser2( List<Long> idList){

        return null;
    }
}