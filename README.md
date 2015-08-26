# webapptomee 

simple web application with some testing servlets
see source for all options: TestServlet, ClusterTestServlet

comes with tomee plugin, to start use

```   
mvn clean package
mvn -Ptomee tomee:run
```

test url (echo "otto" after 2s): e.g. http://localhost:8080/testapp/test?s=2000&echo=otto

yes, tomee is overkill (for now without ee needs) but plugin works better than tomcat variant
apart from being more bloated tomee currently uses tomcat 7 (tomcat 8 is faster in some cases)