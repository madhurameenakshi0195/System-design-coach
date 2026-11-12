package com.sdcoach;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.util.*;
@Service
public class Store {
  public static class Db { public Map<String,Double> mastery=new HashMap<>(); public List<Map<String,Object>> history=new ArrayList<>(); }
  private final ObjectMapper om=new ObjectMapper();
  private final Path file=Path.of("data","db.json");
  public synchronized Db load(){ try{ return Files.exists(file)?om.readValue(file.toFile(),Db.class):new Db(); }catch(Exception e){ return new Db(); } }
  public synchronized void save(Db d){ try{ Files.createDirectories(file.getParent()); om.writerWithDefaultPrettyPrinter().writeValue(file.toFile(),d); }catch(Exception e){ throw new RuntimeException(e); } }
}
