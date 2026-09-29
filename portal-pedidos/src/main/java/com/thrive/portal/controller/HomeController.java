package com.thrive.portal.controller;

import com.thrive.portal.domain.Cliente;
import com.thrive.portal.dto.CadastroClienteForm;
import com.thrive.portal.repository.ClienteRepository;
import com.thrive.portal.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final UsuarioService usuarioService;
    private final ClienteRepository clienteRepository;

    public HomeController(UsuarioService usuarioService, ClienteRepository clienteRepository) {
        this.usuarioService = usuarioService;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registrar")
    public String registrarForm(Model model) {
        model.addAttribute("form", new CadastroClienteForm());
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrar(@Valid @ModelAttribute("form") CadastroClienteForm form,
                            BindingResult br, Model model) {
        // Lab 2.3 - valida na borda; reexibe com mensagens genericas, sem vazar detalhes.
        if (br.hasErrors()) {
            return "registrar";
        }
        // Lab 3.4 - unicidade de e-mail com mensagem generica (anti user enumeration).
        if (usuarioService.porEmail(form.getEmail()).isPresent()) {
            model.addAttribute("mensagem", "Nao foi possivel concluir o cadastro.");
            return "registrar";
        }
        Cliente cliente = new Cliente();
        cliente.setRazaoSocial(form.getRazaoSocial());
        cliente.setCnpj(form.getCnpj());
        cliente.setEmail(form.getEmail());
        cliente = clienteRepository.save(cliente);

        usuarioService.registrar(form.getEmail(), form.getSenha(), "ROLE_USER", cliente.getId());
        model.addAttribute("mensagem", "Cadastro realizado. Faca login.");
        return "login";
    }

    @GetMapping("/esqueci-senha")
    public String esqueciSenhaForm() {
        return "esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String esqueciSenha(@RequestParam String email, Model model) {
        // A07 - user enumeration (mensagem diferente para e-mail existente/inexistente)
        model.addAttribute("mensagem", usuarioService.recuperarSenha(email));
        return "esqueci-senha";
    }
}
