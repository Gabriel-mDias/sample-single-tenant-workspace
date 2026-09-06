package br.com.gems.{app}.{modulo}.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO Espelho — para create/update (PUT/POST body e GET response)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class {Entidade}DTO {

    private String id;              // String no DTO — nunca UUID
    private String nome;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    // TODO: adicionar campos específicos da entidade
}


// DTO de Listagem — somente campos exibidos na tabela (POST /search response)
@Data
@AllArgsConstructor
@NoArgsConstructor
class {Entidade}ResponseDTO {

    private String id;
    private String nome;
    private LocalDateTime dataCriacao;
}


// FilterParams — campos opcionais para filtros de pesquisa (POST /search body)
@Data
@AllArgsConstructor
@NoArgsConstructor
class {Entidade}FilterParams {

    private String nome;

    // TODO: adicionar filtros de range/enum conforme necessidade
    // private String status;
    // private BigDecimal valorMin;
    // private BigDecimal valorMax;
    // private LocalDate dataInicio;
    // private LocalDate dataFim;
}
