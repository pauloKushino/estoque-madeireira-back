package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.ItemVendaRequestDTO;
import com.pi.estoquemadeireira.dto.VendaMapper;
import com.pi.estoquemadeireira.dto.VendaRequestDTO;
import com.pi.estoquemadeireira.dto.VendaResponseDTO;
import com.pi.estoquemadeireira.entity.ItemVenda;
import com.pi.estoquemadeireira.entity.Produto;
import com.pi.estoquemadeireira.entity.Venda;
import com.pi.estoquemadeireira.exception.EstoqueInsuficienteException;
import com.pi.estoquemadeireira.repository.ClienteRepository;
import com.pi.estoquemadeireira.repository.ProdutoRepository;
import com.pi.estoquemadeireira.repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VendaService {

    private static final Logger log = LoggerFactory.getLogger(VendaService.class);

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public VendaService(VendaRepository vendaRepository, ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<VendaResponseDTO> listar() {
        return vendaRepository.findAll().stream()
                .map(VendaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VendaResponseDTO buscarPorId(Long id) {
        return VendaMapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public VendaResponseDTO criar(VendaRequestDTO dto) {
        log.info("Iniciando venda: clienteId={}, itens={}", dto.clienteId(), dto.itens().size());

        // Sem essa validacao, um clienteId inexistente so seria rejeitado pela FK do banco (erro 500)
        if (!clienteRepository.existsById(dto.clienteId())) {
            log.warn("Venda rejeitada: cliente inexistente, clienteId={}", dto.clienteId());
            throw new EntityNotFoundException("Cliente nao encontrado: id " + dto.clienteId());
        }

        Venda venda = new Venda();
        venda.setClienteId(dto.clienteId());
        venda.setDataVenda(LocalDateTime.now());

        // Trava os produtos antes de ler o saldo, sempre em ordem de id: sem isso vendas simultaneas
        // sobrescrevem a baixa uma da outra (lost update) e travam em deadlock no INSERT do item.
        Map<Long, Produto> produtos = new HashMap<>();
        dto.itens().stream().map(ItemVendaRequestDTO::produtoId).distinct().sorted().forEach(id ->
                produtos.put(id, produtoRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado: id " + id))));

        for (ItemVendaRequestDTO itemDTO : dto.itens()) {
            Produto produto = produtos.get(itemDTO.produtoId());

            // Regra 1: rejeita a venda INTEIRA se qualquer item nao tiver estoque
            if (itemDTO.quantidade().compareTo(produto.getQuantidadeEstoque()) > 0) {
                log.warn("Venda rejeitada por estoque insuficiente: produtoId={}, disponivel={}, solicitado={}",
                        produto.getId(), produto.getQuantidadeEstoque(), itemDTO.quantidade());
                throw new EstoqueInsuficienteException(String.format(
                        "Estoque insuficiente para o produto '%s' (id %d): disponivel %s %s, solicitado %s",
                        produto.getNome(), produto.getId(),
                        produto.getQuantidadeEstoque().toPlainString(), produto.getUnidadeMedida(),
                        itemDTO.quantidade().toPlainString()));
            }

            produto.debitarEstoque(itemDTO.quantidade());

            ItemVenda item = new ItemVenda();
            item.setProduto(produto);
            item.setQuantidade(itemDTO.quantidade());
            item.setPrecoUnitarioNoMomento(produto.getPrecoUnitario());
            item.calcularSubtotal();
            venda.adicionarItem(item);
        }

        venda.calcularValorTotal();
        Venda salva = vendaRepository.save(venda);
        log.info("Venda criada: id={}, valorTotal={}", salva.getId(), salva.getValorTotal());
        return VendaMapper.toResponse(salva);
    }

    private Venda buscarEntidade(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada: id " + id));
    }
}
