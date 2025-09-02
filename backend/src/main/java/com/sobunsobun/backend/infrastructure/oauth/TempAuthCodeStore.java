package com.sobunsobun.backend.infrastructure.oauth;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TempAuthCodeStore {
    private static final class Entry { final String json; final long exp; Entry(String j, long e){json=j;exp=e;} }
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void save(String code, String json, Duration ttl){
        store.put(code, new Entry(json, System.currentTimeMillis()+ttl.toMillis()));
    }
    /** 1회용(읽으면 삭제) */
    public String take(String code){
        Entry e = store.remove(code);
        if(e==null || System.currentTimeMillis()>e.exp) return null;
        return e.json;
    }
}
