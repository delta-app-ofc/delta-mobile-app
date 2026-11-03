# Unidades no mobile

Implementação em 02/11/2026. Mantém o nome Unidades e o ícone de prédios do app. A lista mostra os vínculos da conta, a unidade selecionada e o consumo do mês quando disponível. Falha de consumo fica indisponível, sem assumir zero.

## Nova unidade

Campos com label: nome, tipo, categoria residencial, CEP, cidade e estado. Tipo (Casa/Prédio) e categoria (normal/social/especial/favela) exigem seleção explícita, sem padrão. As opções são enviadas conforme os enums de `CreatePropertyRequest` em `ApiModels` e o contrato publicado da API. Cidade/estado vêm da consulta de CEP e são somente leitura; durante a consulta aparece o indicador Consultando.

`UnitsRepository` confirma a conta, cria o endereço por `POST /delta/address` e envia `POST /delta/property` com `userId`, `addressId`, nome, tipo e classificação escolhidos. A criação do imóvel já cria seu vínculo conforme o contrato da rota; não há POST adicional de associação nem fallback para complete-registration. O vínculo retornado por user-properties é conferido antes de abrir o hidrômetro.

O endereço confirmado é reutilizado depois de rejeição determinística da criação do imóvel; nesse estágio o CEP fica bloqueado. Resposta incerta de POST exige conferir a lista antes de iniciar outro cadastro, sem repetir automaticamente uma escrita. O checkpoint conservador no SavedStateHandle evita repetição automática após restauração. Não há transação única entre endereço e imóvel nem remoção automática de endereço órfão.

Após sucesso, abre a tela existente de QR Code/código manual com o ID da unidade criada. O argumento unitFlow mantém a conclusão desse fluxo voltando à lista de Unidades; o fluxo original de cadastro continua com seu destino anterior.

## Exclusão

Popup com identificação da unidade, Cancelar e Excluir. Antes de DELETE /delta/property/{id}, confere novamente o vínculo com a conta. Restrições/dependências rejeitadas pela API são apresentadas; não há exclusão em cascata inventada pelo app. Ao excluir a unidade atual, a seleção e os dados do WaterViewModel são limpos e os vínculos recarregados.

## Validação executada

- Sete testes JVM de UnitsRepositoryTest passaram em execução JUnit direta com dependências locais: seleção explícita, restauração, payload com conta/tipo/categoria, endereço reutilizado, POST incerto sem repetição, exclusão somente de unidade vinculada e consumo indisponível.
- Compilação Java isolada dos componentes alterados e compilação AAPT2 dos layouts/recursos/navegação passaram. A compilação isolada usa R auxiliar e não substitui o build Android.
- Wrapper assembleDebug e testDebugUnitTest direcionado tentados offline: bloqueados por Unable to establish loopback connection. Sem APK novo instalado, validação visual ou cadastro/exclusão reais em produção nesta etapa.

Fontes de implementação: UnitsRepository.java, UnitsViewModel.java, UnitDraft.java, UnitsFragment.java, NewUnitFragment.java, QrScanFragment.java, QrSuccessFragment.java e UnitsRepositoryTest.java.
