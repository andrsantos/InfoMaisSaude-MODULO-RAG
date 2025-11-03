package com.RagArchitecture.InfoMaisSaude.services;

import com.RagArchitecture.InfoMaisSaude.entities.Especialidade;
import com.RagArchitecture.InfoMaisSaude.repositories.EspecialidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EspecialidadeService {

    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    @Autowired
    private RAGIngestionService ragIngestionService; 

    public List<Especialidade> listarTodas() {
        return especialidadeRepository.findAll();
    }

    public Optional<Especialidade> buscarPorId(Long id) {
        return especialidadeRepository.findById(id);
    }

   
    @Transactional 
    public Especialidade criarEspecialidade(Especialidade especialidade) {
        Especialidade novaEspecialidade = especialidadeRepository.save(especialidade);
        
        ragIngestionService.reindexarBaseDeConhecimento();
        
        return novaEspecialidade;
    }

    @Transactional
    public Especialidade atualizarEspecialidade(Long id, Especialidade especialidadeAtualizada) {
        return especialidadeRepository.findById(id).map(especialidade -> {
            especialidade.setNome(especialidadeAtualizada.getNome());
            especialidade.setDescricao(especialidadeAtualizada.getDescricao());
            especialidade.setSintomasComuns(especialidadeAtualizada.getSintomasComuns());
            Especialidade salva = especialidadeRepository.save(especialidade);
            ragIngestionService.reindexarBaseDeConhecimento();
            
            return salva;
        }).orElseThrow(() -> new RuntimeException("Especialidade não encontrada com id: " + id));
    }

    @Transactional
    public void deletarEspecialidade(Long id) {
        if (!especialidadeRepository.existsById(id)) {
            throw new RuntimeException("Especialidade não encontrada com id: " + id);
        }
        especialidadeRepository.deleteById(id);
        ragIngestionService.reindexarBaseDeConhecimento();
    }
}