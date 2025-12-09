package com.RagArchitecture.InfoMaisSaude.services;
import com.RagArchitecture.InfoMaisSaude.entities.Especialidade;
import java.util.List;
import java.util.Optional;

public interface EspecialidadeService {

    List<Especialidade> listarTodas();
    Optional<Especialidade> buscarPorId(Long id);
    Especialidade criarEspecialidade(Especialidade especialidade);
    Especialidade atualizarEspecialidade(Long id, Especialidade especialidadeAtualizada);
    void deletarEspecialidade(Long id);

}