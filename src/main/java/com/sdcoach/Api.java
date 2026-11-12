package com.sdcoach;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@RestController @RequestMapping("/api")
public class Api {
  static class Session { String id; Data.Problem p; int stage; List<String> log=new ArrayList<>(); Map<String,Object> extra=new HashMap<>(); }
  private final Llm llm; private final Store store; private final ObjectMapper om=new ObjectMapper();
  private final Map<String,Session> sessions=new ConcurrentHashMap<>();
  public Api(Llm llm,Store store){ this.llm=llm; this.store=store; }
  private Session get(Map<String,Object> b){ var s=sessions.get(String.valueOf(b.get("id"))); if(s==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"session"); return s; }
  private void log(Session s,String role,String text){ s.log.add("["+Data.STAGES.get(s.stage).name()+"] "+role+": "+text); }
  private String transcript(Session s){ return String.join("\n",s.log); }
  private String recommend(Map<String,Double> m){
    return Data.PROBLEMS.stream().min(Comparator.comparingDouble(p->p.tags().stream().mapToDouble(t->m.getOrDefault(t,2.5)).average().orElse(2.5))).get().id();
  }
  private static double num(Object o,double def){ try{ return Double.parseDouble(String.valueOf(o)); }catch(Exception e){ return def; } }

  @GetMapping("/state") public Map<String,Object> state(){
    var d=store.load(); var m=new LinkedHashMap<String,Object>();
    m.put("live",llm.live()); m.put("stages",Data.STAGES); m.put("problems",Data.PROBLEMS); m.put("mastery",d.mastery);
    m.put("history",d.history.subList(Math.max(0,d.history.size()-8),d.history.size()));
    m.put("recommended",recommend(d.mastery)); m.put("components",Data.COMPONENTS); return m;
  }
  @PostMapping("/start") public Map<String,Object> start(@RequestBody Map<String,Object> b){
    var p=Data.PROBLEMS.stream().filter(x->x.id().equals(b.get("problemId"))).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"problem"));
    var s=new Session(); s.id=UUID.randomUUID().toString(); s.p=p;
    String q=llm.interview(p,0,"",s.extra,null); log(s,"interviewer",q); sessions.put(s.id,s);
    return Map.of("id",s.id,"stage",0,"text",q);
  }
  @PostMapping("/answer") @SuppressWarnings("unchecked") public Map<String,Object> answer(@RequestBody Map<String,Object> b){
    var s=get(b); String text=String.valueOf(b.get("text")); log(s,"learner",text);
    if(b.get("extra") instanceof Map<?,?> x) s.extra=(Map<String,Object>)x;
    String reply=llm.interview(s.p,s.stage,transcript(s),s.extra,text); log(s,"interviewer",reply);
    return Map.of("text",reply);
  }
  @PostMapping("/next") public Map<String,Object> next(@RequestBody Map<String,Object> b){
    var s=get(b); if(s.stage>=Data.STAGES.size()-1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"last stage");
    s.stage++; String q=llm.interview(s.p,s.stage,transcript(s),s.extra,null); log(s,"interviewer",q);
    return Map.of("stage",s.stage,"text",q);
  }
  @PostMapping("/estimate") public Map<String,Object> estimate(@RequestBody Map<String,Object> b){
    double dau=num(b.get("dau"),0),req=num(b.get("req"),0),ratio=num(b.get("ratio"),1),kb=num(b.get("kb"),1),yrs=num(b.get("years"),1);
    double total=dau*req,writes=total/(ratio+1),avg=total/86400,peak=avg*3;
    var r=new LinkedHashMap<String,Object>();
    r.put("avgQps",Math.round(avg)); r.put("peakQps",Math.round(peak));
    r.put("writeQps",Math.round(writes/86400*3)); r.put("readQps",Math.round((total-writes)/86400*3));
    r.put("storageGB",Math.round(writes*kb*365*yrs/1e6*10)/10.0); r.put("bandwidthMbps",Math.round(peak*kb*8/1000*10)/10.0);
    r.put("note",peak>10000?"Peak above ~10k QPS: one DB node will not cope; plan cache + replicas/sharding.":"Peak is modest: one primary plus cache can work; justify any extra complexity.");
    return r;
  }
  @PostMapping("/finish") @SuppressWarnings("unchecked") public Map<String,Object> finish(@RequestBody Map<String,Object> b) throws Exception {
    var s=get(b); String learner=String.join(" ",s.log.stream().filter(l->l.contains("] learner: ")).map(l->l.substring(l.indexOf("learner: ")+9)).toList());
    var r=llm.evaluate(s.p,transcript(s)+"\nBoard: "+om.writeValueAsString(s.extra.getOrDefault("board",List.of())),learner);
    var concepts=(Map<String,Object>)r.getOrDefault("concepts",Map.of()); var d=store.load();
    concepts.forEach((t,v)->{ double old=d.mastery.getOrDefault(t,2.5); d.mastery.put(t,Math.round((old*0.6+num(v,2.5)*0.4)*100)/100.0); });
    int total=((Map<String,Object>)r.get("scores")).values().stream().mapToInt(v->(int)num(v,0)).sum();
    d.history.add(Map.of("date",LocalDate.now().toString(),"problem",s.p.title(),"total",total)); store.save(d);
    var notes=new ArrayList<Map<String,Object>>();
    concepts.forEach((t,v)->{ var k=Data.KB.get(t); if(k!=null&&num(v,5)<=3) notes.add(Map.of("tag",t,"pick",k.pick(),"use",k.use(),"avoid",k.avoid())); });
    r.put("notes",notes); r.put("recommended",recommend(d.mastery)); return r;
  }
  @GetMapping("/drill") public Map<String,Object> drill(){ int i=new Random().nextInt(Data.DRILLS.size()); var q=Data.DRILLS.get(i); return Map.of("i",i,"s",q.s(),"o",q.o()); }
  @PostMapping("/drill") public Map<String,Object> drillCheck(@RequestBody Map<String,Object> b){
    var q=Data.DRILLS.get((int)num(b.get("i"),0)); boolean ok=q.a()==(int)num(b.get("pick"),-1);
    return Map.of("correct",ok,"answer",q.o().get(q.a()),"why",q.why());
  }
}
