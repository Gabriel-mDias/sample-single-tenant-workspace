/** Linha da busca paginada — espelha `ParceiroResponseDTO`, a projeção por construtor da HQL. */
export interface ParceiroResponseModel {
  id: string;
  nome: string;
  cpf?: string;
  email?: string;
  estadoSigla?: string;
  /** `null` para quem não tem `VISUALIZAR_COMISSAO`. O backend apaga a coluna linha a linha. */
  comissao?: number | null;
  solicitacoesConcluidas?: number;
}
