package com.example.demo;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.demo")
public class DemoApplication {

  @Value("${server.port:80}")
  private String serverPort = "0";
  private String contextPath = "/camel";

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Bean
  ServletRegistrationBean servletRegistrationBean() {
    ServletRegistrationBean servlet = new ServletRegistrationBean(new CamelHttpTransportServlet(), contextPath + "/*");
    servlet.setName("CamelServlet");
    return servlet;
  }


  @Component
  class RestApi extends RouteBuilder {

    @Autowired
    private ObjectMapper mapper;

//    @Autowired
//    private RestTemplate restTemplate;

    @Override
    public void configure() {

      CamelContext context = new DefaultCamelContext();


      // http://localhost:8080/camel/api-doc
      restConfiguration().contextPath(contextPath) //
          .port(serverPort)
          .enableCORS(true)
          .apiContextPath("/api-doc")
          .apiProperty("api.title", "Test REST API")
          .apiProperty("api.version", "v1")
          .apiProperty("cors", "true") // cross-site
          .apiContextRouteId("doc-api")
          .component("servlet")
          .bindingMode(RestBindingMode.json)
          .dataFormatProperty("prettyPrint", "true");

/**
 The Rest DSL supports automatic binding json/xml contents to/from
 POJOs using Camels Data Format.
 By default the binding mode is off, meaning there is no automatic
 binding happening for incoming and outgoing messages.
 You may want to use binding if you develop POJOs that maps to
 your REST services request and response types.
 */

      rest("/api/").description("Teste REST Service")
          .id("api-route")
          .post("/bean")
          .produces(MediaType.APPLICATION_JSON_VALUE)
          .consumes(MediaType.APPLICATION_JSON_VALUE)
          .bindingMode(RestBindingMode.auto)
          .type(MyBean.class)
          .enableCORS(true)

          .to("direct:remoteService");

      rest("/api/").description("Teste REST Service")
          .id("list-route-plain")
          .get("/listPlain")
          .param().name("cpf").required(true).dataType("number").type(RestParamType.query).endParam()
          .produces(MediaType.APPLICATION_JSON_VALUE)
          .outType(MyBean.class)
          .to("direct:getList");

      rest("/api/").description("Teste REST Service")
          .id("list-item-route-plain")
          .get("/listItem")
          .param().name("id").required(true).dataType("number").type(RestParamType.query).endParam()
          .produces(MediaType.APPLICATION_JSON_VALUE)
          .outType(MyBean.class)
          .to("direct:getListItem");

      rest("/api/").description("Teste REST Service")
          .id("list-route")
          .get("/list")
          .param().name("cpf").required(true).dataType("number").type(RestParamType.query).endParam()
          .param().name("expand").required(false).dataType("string").allowableValues("bla").type(RestParamType.query).endParam()
          .produces(MediaType.APPLICATION_JSON_VALUE)
          .route()
          .removeHeaders("CamelHttp*")
          .toD("http4:localhost:8080/camel/api/listPlain?cpf=${header.cpf}")
          .process(exchange -> {

            //   final MyBean[] beansA = exchange.getIn().getBody(MyBean[].class);
            // final List<MyBean> list = Arrays.asList(beansA);

            final String body = exchange.getIn().getBody(String.class);

            //final List<MyBean> beans = mapper.readValue(body, new TypeReference<List<MyBean>>() {});
            final List<MyBean> beans = mapper.readValue(body,
                mapper.getTypeFactory().constructCollectionType(List.class, MyBean.class));

            exchange.getIn().setBody(beans);

          })
          .choice()
          .when(exchange -> "bla".equals(exchange.getIn().getHeader("expand"))).to("direct:expandItems")
          .end()
          .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

      from("direct:getList")
          .routeId("direct-route-list")
          .tracing()
          .process(exchange -> {
            final String cpf = exchange.getIn().getHeader("cpf").toString();
            exchange.getIn().setBody(ExampleServices.list(cpf));
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

      from("direct:getListItem")
          .routeId("direct-route-list-item")
          .tracing()
          .process(exchange -> {
            final String id = exchange.getIn().getHeader("id").toString();
            exchange.getIn().setBody(ExampleServices.listItems(id));
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

      from("direct:expandItems")
          .routeId("direct-route-expand")
          .tracing()

          // Usando Split (não consegui atualizar a lista pai, mas consegui buscar os itens!  :(
//          .process(exchange -> {
//            final List<MyBean> beans = (List<MyBean>) exchange.getIn().getBody();
//            final Map<Integer, MyBean> map = beans.stream()
//                .collect(Collectors.toMap(myBean -> myBean.getId(), myBean -> myBean));
//            exchange.getProperty("map", map);
//          })
//          .split(body())
//          .log(body().toString())
//          .process(exchange -> {
//            final MyBean bean = exchange.getIn().getBody(MyBean.class);
//            exchange.getIn().setBody(null);
//            exchange.getIn().setHeader("bodyId", bean.getId());
//          })
          //.removeHeaders("CamelHttp*")
          //.enrich("http4:localhost:8080/camel/api/listItem?id=777")
//          .toD("http4:localhost:8080/camel/api/listItem?id=${header.bodyId}")
//          .process(exchange -> {
//            final String body = exchange.getIn().getBody(String.class);
//            log.debug("teste2: " + body);
//            final List<String> items = mapper.readValue(body,
//                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
//            exchange.getIn().setBody(items);
//          })
//          .end()

          // Usando RestTemplate
          .process(exchange -> {
            //noinspection unchecked
            final List<MyBean> beans = (List<MyBean>) exchange.getIn().getBody();
            final RestTemplate restTemplate = new RestTemplate();
            final String baseUrl = "http://localhost:8080/camel/api/listItem?id=";
            beans.parallelStream().forEach(bean -> {
              final ResponseEntity<String[]> responseEntity = restTemplate.getForEntity(baseUrl + bean.getId(), String[].class);
              final List<String> items = Arrays.asList(responseEntity.getBody());
              bean.setItems(items);
            });
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

      from("direct:remoteService")
          .routeId("direct-route")
          .tracing()
          .log(">>> ${body.id}")
          .log(">>> ${body.name}")
//                .transform().simple("blue ${in.body.name}")
          .process(exchange -> {
            MyBean bodyIn = (MyBean) exchange.getIn().getBody();

            ExampleServices.example(bodyIn);

            exchange.getIn().setBody(bodyIn);
          })
          .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201));

    }
  }
}
