package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.models.UserSession;
import org.springframework.stereotype.Service;

@Service
public interface SessionService {

     UserSession getOrCreateSession(String telefone);
     void clearSession(String telefone);
     UserSession getSession(String telefone);


}