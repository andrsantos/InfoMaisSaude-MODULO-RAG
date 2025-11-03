
document.addEventListener("DOMContentLoaded", () => {
    
    const navHome = document.getElementById("navHome");
    const navAdmin = document.getElementById("navAdmin");
    const pageHome = document.getElementById("pageHome");
    const pageAdmin = document.getElementById("pageAdmin");

    const ragForm = document.getElementById("ragForm");
    const sintomasInput = document.getElementById("sintomasInput");
    const ragSubmitBtn = document.getElementById("ragSubmitBtn");
    const ragResult = document.getElementById("ragResult");
    const ragResponseText = document.getElementById("ragResponseText");

    const adminForm = document.getElementById("adminForm");
    const adminNome = document.getElementById("adminNome");
    const adminDescricao = document.getElementById("adminDescricao");
    const adminSintomas = document.getElementById("adminSintomas");
    const adminSubmitBtn = document.getElementById("adminSubmitBtn");
    const especialidadesLista = document.getElementById("especialidadesLista");

    const editModalOverlay = document.getElementById("editModalOverlay");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const editForm = document.getElementById("editForm");
    const editId = document.getElementById("editId");
    const editNome = document.getElementById("editNome");
    const editDescricao = document.getElementById("editDescricao");
    const editSintomas = document.getElementById("editSintomas");
    const editSubmitBtn = document.getElementById("editSubmitBtn");

    const API_MEDICO = "/api/medico";
    const API_ADMIN = "/api/admin/especialidades";


    navHome.addEventListener("click", () => {
        pageHome.style.display = "block";
        pageAdmin.style.display = "none";
        navHome.classList.add("active");
        navAdmin.classList.remove("active");
    });

    navAdmin.addEventListener("click", () => {
        pageHome.style.display = "none";
        pageAdmin.style.display = "block";
        navHome.classList.remove("active");
        navAdmin.classList.add("active");
        loadEspecialidades();
    });


    ragForm.addEventListener("submit", async (e) => {
        e.preventDefault(); 
        const sintomas = sintomasInput.value;
        if (!sintomas) return;

        ragSubmitBtn.disabled = true;
        ragSubmitBtn.textContent = "Analisando...";
        ragResult.style.display = "none";

        try {
            const response = await fetch(`${API_MEDICO}/recomendar`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ sintomas: sintomas })
            });
            if (!response.ok) throw new Error(`Erro da API: ${response.statusText}`);
            const recomendacao = await response.text();
            ragResponseText.textContent = recomendacao;
            ragResult.style.display = "block";
        } catch (error) {
            ragResponseText.textContent = `Erro ao conectar com o backend. \nDetalhe: ${error.message}`;
            ragResult.style.display = "block";
        } finally {
            ragSubmitBtn.disabled = false;
            ragSubmitBtn.textContent = "Obter Recomendação";
        }
    });


    async function loadEspecialidades() {
        especialidadesLista.innerHTML = "<li>Carregando...</li>";
        try {
            const response = await fetch(API_ADMIN);
            const data = await response.json();
            
            especialidadesLista.innerHTML = ""; 
            if (data.length === 0) {
                 especialidadesLista.innerHTML = "<li>Nenhuma especialidade cadastrada.</li>";
                 return;
            }

            data.forEach(esp => {
                const li = document.createElement("li");
                
                li.innerHTML = `
                    <button class="delete-btn" data-id="${esp.id}">X</button>
                    
                    <button class="edit-btn" 
                        data-id="${esp.id}" 
                        data-nome="${esp.nome}" 
                        data-descricao="${esp.descricao}" 
                        data-sintomas="${esp.sintomasComuns}">
                        ✏️ Editar
                    </button>
                    
                    <strong>${esp.nome}</strong>
                    <p>${esp.descricao}</p>
                    <small>Sintomas: ${esp.sintomasComuns}</small>
                `;
                especialidadesLista.appendChild(li);
            });
            
            document.querySelectorAll('.delete-btn').forEach(btn => 
                btn.addEventListener('click', handleDelete)
            );
            document.querySelectorAll('.edit-btn').forEach(btn => 
                btn.addEventListener('click', handleOpenEditModal)
            );

        } catch (error) {
            especialidadesLista.innerHTML = `<li>Erro ao carregar especialidades: ${error.message}</li>`;
        }
    }

    adminForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const novaEspecialidade = {
            nome: adminNome.value,
            descricao: adminDescricao.value,
            sintomasComuns: adminSintomas.value
        };
        adminSubmitBtn.disabled = true;
        adminSubmitBtn.textContent = "Salvando...";
        try {
            await fetch(API_ADMIN, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(novaEspecialidade)
            });
            adminNome.value = "";
            adminDescricao.value = "";
            adminSintomas.value = "";
            loadEspecialidades(); 
        } catch (error) {
            alert(`Erro ao salvar: ${error.message}`);
        } finally {
            adminSubmitBtn.disabled = false;
            adminSubmitBtn.textContent = "Salvar e Reindexar";
        }
    });
    
    async function handleDelete(e) {
        const id = e.target.getAttribute('data-id');
        if (!confirm(`Tem certeza que deseja deletar a especialidade com ID ${id}?`)) {
            return;
        }
        try {
            await fetch(`${API_ADMIN}/${id}`, { method: "DELETE" });
            loadEspecialidades(); 
        } catch (error) {
             alert(`Erro ao deletar: ${error.message}`);
        }
    }


    function handleOpenEditModal(e) {
        const btn = e.target;
        const id = btn.getAttribute('data-id');
        const nome = btn.getAttribute('data-nome');
        const descricao = btn.getAttribute('data-descricao');
        const sintomas = btn.getAttribute('data-sintomas');
        
        editId.value = id;
        editNome.value = nome;
        editDescricao.value = descricao;
        editSintomas.value = sintomas;
        
        editModalOverlay.style.display = "flex";
    }

    function handleCloseModal() {
        editModalOverlay.style.display = "none";
    }
    
    async function handleUpdate(e) {
        e.preventDefault();
        
        const id = editId.value;
        const updatedEspecialidade = {
            id: id,
            nome: editNome.value, 
            descricao: editDescricao.value,
            sintomasComuns: editSintomas.value
        };

        editSubmitBtn.disabled = true;
        editSubmitBtn.textContent = "Salvando...";

        try {
            await fetch(`${API_ADMIN}/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(updatedEspecialidade)
            });
            
            handleCloseModal();
            loadEspecialidades(); 

        } catch (error) {
            alert(`Erro ao atualizar: ${error.message}`);
        } finally {
            editSubmitBtn.disabled = false;
            editSubmitBtn.textContent = "Salvar Alterações e Reindexar";
        }
    }

    closeModalBtn.addEventListener('click', handleCloseModal);
    editModalOverlay.addEventListener('click', (e) => {
        if (e.target === editModalOverlay) {
            handleCloseModal();
        }
    });
    editForm.addEventListener('submit', handleUpdate);

}); 