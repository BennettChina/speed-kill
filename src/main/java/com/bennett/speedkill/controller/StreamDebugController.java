package com.bennett.speedkill.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试Stream流在idea中debug是否会报流已关闭异常
 *
 * 测试结果：不会（可能已处理该问题）
 *
 * @author bennett
 * @date 2020/11/15
 */

@RestController
public class StreamDebugController {

    @GetMapping("/stream/debug/test")
    public String streamDebug() {
        String[] strings = {"111", "222", "333", "444", "555"};
        List<String> list = new LinkedList<>(Arrays.asList(strings));
        List<String> collect = list.stream().filter("444"::equals).collect(Collectors.toList());
        collect.forEach(System.out::println);
        return "ok";
    }
}
