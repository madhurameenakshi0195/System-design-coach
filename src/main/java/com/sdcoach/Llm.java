package com.sdcoach;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.*;
import java.util.regex.Pattern;
@Service
public class Llm {
  @Value("${bharatcode.api-key:}") String key;
  @Value("${bharatcode.base-url:}") String base;
  @Value("${bharatcode.model:}") String model;
  private final ObjectMapper om=new ObjectMapper();
  private final RestClient rest=RestClient.create();
  static final String INTERVIEWER="You are a strict but fair FAANG system design interviewer. Stay inside the current stage. Ask ONE question at a time, max 70 words. Never give the answer. If the learner's numbers or design have a flaw, point at it with a question. If the stage is covered, say so and tell them to move on.";
  static final String EVALUATOR="You are a system design evaluator. Reply with ONLY JSON: {\"scores\":{\"requirements\":0-5,\"estimation\":0-5,\"components\":0-5,\"data\":0-5,\"scaling\":0-5,\"tradeoffs\":0-5},\"concepts\":{\"<tag>\":0-5},\"strengths\":[\"...\"],\"gaps\":[\"...\"],\"summary\":\"2 sentences\"}. Use only the concept tags provided. Base scores strictly on evidence in the transcript.";
  public boolean live(){ return !key.isBlank()&&!base.isBlank()&&!model.isBlank(); }
  private String call(String system,String user){
    var body=Map.of("model",model,"temperature",0.3,"messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",user)));
    JsonNode r=rest.post().uri(base.replaceAll("/$","")+"/chat/completions").header("Authorization","Bearer "+key).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
    return r.path("choices").path(0).path("message").path("content").asText();
  }
  public String interview(Data.Problem p,int stage,String transcript,Object extra,String learner){
    var st=Data.STAGES.get(stage);
    if(live()) try{
      return call(INTERVIEWER,"Problem: "+p.title()+" - "+p.brief()+"\nStage: "+st.name()+"\nExtra data (calculator/board): "+om.writeValueAsString(extra)+"\nTranscript:\n"+transcript+"\nLearner just said: "+(learner==null?"(stage just started, ask your opening question)":learner));
    }catch(Exception e){ /* fall back to demo mode */ }
    if(learner==null) return st.q();
    String[] hints={"Which requirement would you drop first if you had to? Why?","Does your peak QPS assume average or peak traffic? Check the ratio you used.","You named components, but why that store over the alternative?","What happens to that component under two concurrent writers?","And if the replica lags behind the primary during that failure?"};
    return hints[stage]+" (demo mode: connect BharatCode for real follow-ups)";
  }
  public Map<String,Object> evaluate(Data.Problem p,String transcript,String learnerText){
    if(live()) try{
      String t=call(EVALUATOR,"Problem: "+p.title()+"\nConcept tags: "+String.join(", ",p.tags())+"\nTranscript:\n"+transcript);
      return om.readValue(t.replaceAll("```json|```","").trim(),new TypeReference<Map<String,Object>>(){});
    }catch(Exception e){ /* fall back to demo mode */ }
    String lc=learnerText.toLowerCase(); int words=learnerText.isBlank()?0:learnerText.trim().split("\\s+").length;
    int base=Math.min(5,Math.round(words/60f));
    Map<String,Object> scores=new LinkedHashMap<>();
    scores.put("requirements",Math.min(5,base+hit(lc,"latency|availability|consisten")));
    scores.put("estimation",Math.min(5,base+hit(lc,"qps|storage|gb")));
    scores.put("components",base);
    scores.put("data",Math.min(5,base+hit(lc,"schema|postgres|cassandra|index")));
    scores.put("scaling",Math.min(5,base+hit(lc,"shard|replica|cache|queue")));
    scores.put("tradeoffs",Math.min(5,base+hit(lc,"because|trade|instead|vs")));
    Map<String,Object> concepts=new LinkedHashMap<>();
    for(String t:p.tags()){ String first=Data.KB.get(t).pick().split("[ /]")[0].toLowerCase();
      int bonus=(lc.contains(t.replace('-',' '))||lc.contains(first))?1:0; concepts.put(t,Math.max(1,Math.min(5,base+bonus))); }
    Map<String,Object> r=new LinkedHashMap<>();
    r.put("scores",scores); r.put("concepts",concepts);
    r.put("strengths",List.of("Demo-mode heuristic score based on answer depth and keywords."));
    r.put("gaps",List.of("Connect BharatCode for evidence-based gaps."));
    r.put("summary","Offline demo scoring. With a live model this becomes a quoted, evidence-based review.");
    return r;
  }
  private static int hit(String s,String re){ var m=Pattern.compile(re).matcher(s); int c=0; while(m.find()) c++; return c; }
}
