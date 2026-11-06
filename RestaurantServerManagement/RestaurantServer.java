import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class RestaurantServer {
    static class MenuItem {
        int id; String name, category; double price;
        MenuItem(int id, String name, String category, double price) {
            this.id=id; this.name=name; this.category=category; this.price=price;
        }
    }
    static class RestaurantTable {
        int id, capacity; String status;
        RestaurantTable(int id, int capacity, String status) { this.id=id; this.capacity=capacity; this.status=status; }
    }
    static class Order {
        int id, tableId; String customer, status; List<String> items; double total;
        Order(int id, int tableId, String customer, String status, List<String> items, double total) {
            this.id=id; this.tableId=tableId; this.customer=customer; this.status=status; this.items=items; this.total=total;
        }
    }

    static final List<MenuItem> menu = new ArrayList<>();
    static final List<RestaurantTable> tables = new ArrayList<>();
    static final List<Order> orders = new ArrayList<>();
    static int nextOrderId = 1;

    public static void main(String[] args) throws Exception {
        menu.add(new MenuItem(1, "Veg Burger", "Snacks", 120));
        menu.add(new MenuItem(2, "Margherita Pizza", "Main Course", 250));
        menu.add(new MenuItem(3, "Masala Dosa", "Breakfast", 80));
        menu.add(new MenuItem(4, "Paneer Biryani", "Main Course", 180));
        menu.add(new MenuItem(5, "Fresh Lime Soda", "Drinks", 50));
        tables.add(new RestaurantTable(1, 2, "Available"));
        tables.add(new RestaurantTable(2, 4, "Available"));
        tables.add(new RestaurantTable(3, 4, "Occupied"));
        tables.add(new RestaurantTable(4, 6, "Available"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", RestaurantServer::home);
        server.createContext("/api/menu", RestaurantServer::getMenu);
        server.createContext("/api/tables", RestaurantServer::getTables);
        server.createContext("/api/orders", RestaurantServer::handleOrders);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Restaurant Management System is running.");
        System.out.println("Open http://localhost:8080 in your browser.");
    }

    // Finds index.html. First tries the classpath exactly as before; if it is not there (for example when the
    // program is started with the VS Code "Run" button, which puts the .class files in a separate bin/out folder),
    // it falls back to index.html on disk: the current folder, the folder of the compiled classes, or its parents.
    static InputStream openIndexHtml() throws IOException {
        InputStream in = RestaurantServer.class.getResourceAsStream("/index.html");
        if (in != null) return in;
        List<java.nio.file.Path> places = new ArrayList<>();
        places.add(java.nio.file.Paths.get("index.html"));
        places.add(java.nio.file.Paths.get(System.getProperty("user.dir", "."), "index.html"));
        try {
            java.nio.file.Path here = java.nio.file.Paths.get(
                RestaurantServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (int i = 0; i < 3 && here != null; i++) {
                places.add((java.nio.file.Files.isDirectory(here) ? here : here.getParent()).resolve("index.html"));
                here = here.getParent();
            }
        } catch (Exception ignored) { }
        for (java.nio.file.Path p : places) {
            if (java.nio.file.Files.isRegularFile(p)) return java.nio.file.Files.newInputStream(p);
        }
        return null;
    }

    static void home(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, "text/plain", "Method not allowed"); return; }
        try (InputStream in = openIndexHtml()) {
            if (in == null) { send(ex, 500, "text/plain", "index.html not found. Keep it in the same project resources folder."); return; }
            byte[] bytes = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out=ex.getResponseBody()) { out.write(bytes); }
        }
    }

    static synchronized void getMenu(HttpExchange ex) throws IOException {
        StringBuilder s = new StringBuilder("[");
        for (int i=0;i<menu.size();i++) {
            MenuItem m=menu.get(i);
            if(i>0)s.append(",");
            s.append("{\"id\":").append(m.id).append(",\"name\":\"").append(escape(m.name))
             .append("\",\"category\":\"").append(escape(m.category)).append("\",\"price\":").append(m.price).append("}");
        }
        s.append("]");
        send(ex,200,"application/json; charset=UTF-8",s.toString());
    }

    static synchronized void getTables(HttpExchange ex) throws IOException {
        StringBuilder s = new StringBuilder("[");
        for(int i=0;i<tables.size();i++){
            RestaurantTable t=tables.get(i); if(i>0)s.append(",");
            s.append("{\"id\":").append(t.id).append(",\"capacity\":").append(t.capacity)
             .append(",\"status\":\"").append(escape(t.status)).append("\"}");
        }
        s.append("]");
        send(ex,200,"application/json; charset=UTF-8",s.toString());
    }

    static synchronized void handleOrders(HttpExchange ex) throws IOException {
        if ("GET".equals(ex.getRequestMethod())) {
            StringBuilder s=new StringBuilder("[");
            for(int i=0;i<orders.size();i++){
                Order o=orders.get(i); if(i>0)s.append(",");
                s.append("{\"id\":").append(o.id).append(",\"tableId\":").append(o.tableId)
                 .append(",\"customer\":\"").append(escape(o.customer)).append("\",\"status\":\"")
                 .append(escape(o.status)).append("\",\"items\":\"").append(escape(String.join(", ",o.items)))
                 .append("\",\"total\":").append(o.total).append("}");
            }
            s.append("]");
            send(ex,200,"application/json; charset=UTF-8",s.toString());
            return;
        }
        if (!"POST".equals(ex.getRequestMethod())) { send(ex,405,"application/json","{\"error\":\"Use GET or POST\"}"); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> p = parseForm(body);
        try {
            String customer=p.getOrDefault("customer","").trim();
            int tableId=Integer.parseInt(p.getOrDefault("tableId","0"));
            String[] itemIds=p.getOrDefault("items","").split(",");
            if(customer.isEmpty()) throw new IllegalArgumentException("Please enter the customer name.");
            RestaurantTable table=null;
            for(RestaurantTable t:tables) if(t.id==tableId)table=t;
            if(table==null) throw new IllegalArgumentException("Please select a valid table.");
            if(!"Available".equals(table.status)) throw new IllegalArgumentException("That table is not available. Choose another table.");
            double total=0; List<String> chosen=new ArrayList<>();
            for(String raw:itemIds){
                if(raw.trim().isEmpty()) continue;
                int id=Integer.parseInt(raw.trim());
                for(MenuItem m:menu) if(m.id==id){ total+=m.price; chosen.add(m.name); }
            }
            if(chosen.isEmpty()) throw new IllegalArgumentException("Please select at least one menu item.");
            Order o=new Order(nextOrderId++,tableId,customer,"Preparing",chosen,total);
            orders.add(o); table.status="Occupied";
            send(ex,200,"application/json; charset=UTF-8",
                "{\"message\":\"Order placed successfully!\",\"orderId\":"+o.id+",\"total\":"+o.total+"}");
        } catch (Exception e) {
            send(ex,400,"application/json; charset=UTF-8","{\"error\":\""+escape(e.getMessage()==null?"Invalid order":e.getMessage())+"\"}");
        }
    }

    static Map<String,String> parseForm(String body) throws UnsupportedEncodingException {
        Map<String,String> map=new HashMap<>();
        for(String pair:body.split("&")){
            String[] kv=pair.split("=",2);
            String key=URLDecoder.decode(kv[0],"UTF-8");
            String value=kv.length>1?URLDecoder.decode(kv[1],"UTF-8"):"";
            map.put(key,value);
        }
        return map;
    }
    static String escape(String s) {
        if(s==null)return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ").replace("\r"," ");
    }
    static void send(HttpExchange ex,int status,String type,String body)throws IOException{
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.sendResponseHeaders(status,bytes.length);
        try(OutputStream out=ex.getResponseBody()){out.write(bytes);}
    }
}
