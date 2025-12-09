package com.RagArchitecture.InfoMaisSaude.services.impl;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.RagArchitecture.InfoMaisSaude.models.UserSession;
import com.RagArchitecture.InfoMaisSaude.services.SessionService;

@Service
public class SessionServiceImpl implements SessionService {

    private final ConcurrentHashMap<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    public UserSession getOrCreateSession(String telefone) {
        return activeSessions.computeIfAbsent(telefone, k -> new UserSession(telefone));
    }

    public void clearSession(String telefone) {
        activeSessions.remove(telefone);
    }
    
    public UserSession getSession(String telefone) {
        return activeSessions.get(telefone);
    }
    
}
