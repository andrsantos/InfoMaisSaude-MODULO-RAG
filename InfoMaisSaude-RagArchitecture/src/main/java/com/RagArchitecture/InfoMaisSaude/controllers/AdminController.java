package com.RagArchitecture.InfoMaisSaude.controllers;

import com.RagArchitecture.InfoMaisSaude.entities.Especialidade;
import com.RagArchitecture.InfoMaisSaude.services.EspecialidadeService;
import com.RagArchitecture.InfoMaisSaude.services.RAGIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") 
public class AdminController {

    @Autowired
    private RAGIngestionService ingestionService;

    @Autowired
    private EspecialidadeService especialidadeService; 

    
    @PostMapping("/reindexar")
    public ResponseEntity<String> reindexar() {
        try {
            ingestionService.reindexarBaseDeConhecimento();
            return ResponseEntity.ok("Reindexação concluída com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro durante a reindexação: " + e.getMessage());
        }
    }

    @PostMapping("/especialidades")
    public ResponseEntity<Especialidade> criarEspecialidade(@RequestBody Especialidade especialidade) {
        Especialidade novaEspecialidade = especialidadeService.criarEspecialidade(especialidade);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaEspecialidade);
    }


    @GetMapping("/especialidades")
    public ResponseEntity<List<Especialidade>> listarEspecialidades() {
        return ResponseEntity.ok(especialidadeService.listarTodas());
    }

    @PutMapping("/especialidades/{id}")
    public ResponseEntity<Especialidade> atualizarEspecialidade(@PathVariable Long id, @RequestBody Especialidade especialidade) {
        Especialidade atualizada = especialidadeService.atualizarEspecialidade(id, especialidade);
        return ResponseEntity.ok(atualizada);
    }


    @DeleteMapping("/especialidades/{id}")
    public ResponseEntity<Void> deletarEspecialidade(@PathVariable Long id) {
        especialidadeService.deletarEspecialidade(id);
        return ResponseEntity.noContent().build(); 
    }
}