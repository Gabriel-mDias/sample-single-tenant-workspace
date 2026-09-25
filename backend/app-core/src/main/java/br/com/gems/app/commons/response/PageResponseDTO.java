package br.com.gems.app.commons.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** Contrato JSON estável das buscas paginadas consumidas pelo frontend. */
public record PageResponseDTO<T>(List<T> content, long totalElements, int totalPages, int number, int size) {

    public static <T> PageResponseDTO<T> from( Page<T> page ) {
        return new PageResponseDTO<>( page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize() );
    }

}
