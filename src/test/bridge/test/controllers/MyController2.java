package bridge.test.controllers;

import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("test")
public class MyController2 <E>{


    enum EM{
        name,age
    }
    public static class GenericTypeOf<T> {//测试 内部类
        T t;

        //开始时间
        Date startTime;

        EM em;

        public EM getEm() {
            return em;
        }

        public GenericTypeOf<T> setEm(EM em) {
            this.em = em;
            return this;
        }

        public Date getStartTime() {
            return startTime;
        }

        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }

        public T getT() {
            return t;
        }

        public GenericTypeOf<T> setT(T t) {
            this.t = t;
            return this;
        }
    }


    @GetMapping("array")
    String[] array(List<Long> idList) {
        return null;
    }

    @GetMapping("baseArray")
    int[] baseArray(List<Long> idList) {
        return null;
    }

    @GetMapping("users")
    GenericTypeOf<GenericTypeOf<E>> getUser(List<Long> idList) {
        return null;
    }

    @PostMapping("users/{id}")
    List<Long> pathForList(@RequestBody List<Long> idList, @PathVariable("id") Long orId) {
        return null;
    }

    @GetMapping("getUser4")
    Map<Long, Map<String, Long>> getUser4(List<Long> idList) {

        return null;
    }

    <T> List<T> getUser3() {

        return null;
    }

    @GetMapping("users")
    List<Long> getUser2(List<Long> idList) {

        return null;
    }
}