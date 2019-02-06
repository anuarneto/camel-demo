package com.example.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExampleServices {

  public static void example(MyBean bodyIn) {
    bodyIn.setName("Hello, " + bodyIn.getName());
    bodyIn.setId(bodyIn.getId() * 10);
  }

  public static List<MyBean> list(final String arg) {

    final List<MyBean> beanList = new ArrayList<>(5);
    for (int i = 1; i < 6; i++) {
      final MyBean bean = new MyBean();
      bean.setName("Hello, arg = \"" + arg + "\"..." + i);
      bean.setId(i * 10);
      beanList.add(bean);
    }
    return beanList;
  }

  public static List<String> listItems(final String id) {
    return Arrays.asList(
        "item id " + id + ".1",
        "item id " + id + ".2",
        "item id " + id + ".3",
        "item id " + id + ".4"
    );
  }
}
