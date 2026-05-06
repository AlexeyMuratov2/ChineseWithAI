# Аудит архитектуры модулей уроков

Дата аудита: 2026-05-05

## Цель

Зафиксировать, насколько текущая архитектура готова к добавлению новых модулей уроков, где каждый модуль может иметь:

- собственную структуру итогового lesson JSON;
- собственный системный промпт и правила генерации;
- собственный процесс создания урока: one-shot, multi-step, stage artifacts, repair, валидация;
- собственные требования к draft input, словарю, review flow и отображению на фронтенде.

Ключевой вывод: архитектура уже движется в правильную сторону, но сейчас она готова к масштабированию только для модулей, которые остаются внутри общего lesson envelope. Для "абсолютно разных уроков", отличающихся и структурой, и процессом создания, нужно формализовать контракт модуля как отдельный extension point, вынести HSK5-специфику из общего слоя и сделать workflow декларативным или явно модульным.

## Краткая оценка готовности

| Область | Оценка | Комментарий |
| --- | ---: | --- |
| Добавить новый модуль с похожей структурой | 7/10 | Есть `LessonModule`, `LessonModuleStrategy`, agent profiles, system prompt appendix, output validation и one-shot generation. |
| Добавить новый модуль с другими section types | 6/10 | Можно через новую `LessonModuleStrategy`, но общий `LessonContentValidator` все равно требует `newWords`, `sections`, языки и schema envelope. |
| Добавить модуль с радикально другой top-level структурой | 3/10 | Текущий validator, persister и vocabulary tracking завязаны на обязательные top-level поля. |
| Подменить workflow в зависимости от модуля | 6/10 | `generatorWorkflowVariantKey` и `generationPipelineKey` уже есть, но multi-step pipeline пока пишется Java-классом под конкретный HSK5 flow. |
| Подменить prompt/contract на уровне модуля | 6/10 | Prompt appendix хранится в БД, но контракт раздвоен между БД, Java strategy, output contract и stage validators. |
| Наблюдаемость генерации | 7/10 | `lesson_generation_runs` и stage trace - сильная база. Не хватает универсальной модели stage input/output и contract snapshots. |
| Готовность к долгосрочному масштабированию | 5/10 | Архитектурные зачатки хорошие, но contract-first модульной платформы пока нет. |

Итоговая оценка: 5.5/10 для задачи "поддерживать абсолютно разные уроки и разные процессы создания". Для текущих HSK5/TestModule сценариев архитектура рабочая. Для продуктовой платформы модулей нужен следующий слой абстракций.

## Текущая архитектурная карта

### Основные bounded contexts

Проект использует Spring Modulith:

- `lesson` зависит от `lessondraft::application` и `agentruntime::application`.
- `agentruntime` является отдельным runtime для запуска agent sessions, prompt/context build, pre-generation workflow, tool calls и final output validation.
- `learnerprofile`, `teacherpersonality`, vocabulary review подключаются как pre-generation context steps.

Это хорошее направление: lesson module не должен знать детали конкретного LLM gateway, а agent runtime не должен знать предметные правила каждого урока.

### Lesson module

Текущая сущность `LessonModule` содержит:

- `moduleKey`;
- `displayName`;
- `systemPromptAppendix`;
- `schemaVersion`;
- `active`;
- `generatorProfileKey`;
- `generatorWorkflowVariantKey`;
- `generationPipelineKey`.

Это уже похоже на runtime-конфигурацию модуля. Главное ограничение: это еще не полный контракт модуля. В нем нет machine-readable schema, draft policy, vocabulary policy, rendering hints, default model policy, stage plan и тестового contract fixture.

### Lesson generation

Сейчас есть два пути генерации:

1. One-shot generation:
   - `LessonApplicationService.generateFromDraft(...)`;
   - берет `module.generatorProfileKey()`;
   - строит input через `LessonGenerationInputFactory`;
   - добавляет prompt через `LessonGenerationPromptFactory`;
   - передает `module.generatorWorkflowVariantKey()`;
   - валидирует итог через `LessonContentValidator`;
   - сохраняет через `GeneratedLessonPersister`.

2. Pipeline generation:
   - если у модуля задан `generationPipelineKey`, `LessonApplicationService` идет в `LessonGenerationPipelineCatalog`;
   - pipeline сам запускает stage agent sessions;
   - HSK5 реализован в `Hsk5QualityLessonGenerationPipeline`;
   - stage trace пишется через `LessonGenerationTraceRepository`;
   - итог снова проходит через общий `LessonContentValidator`.

Это хорошая основа для подмены workflow. Но HSK5 pipeline сейчас является hardcoded orchestration class, а не универсальным workflow definition.

### Agent runtime

`agentruntime` содержит сильные extension points:

- `AgentProfile` для system prompt, context builder, tools, execution policy, output contract, repair flag;
- `PreGenerationWorkflow` и `PreGenerationStep`;
- `AgentContextBuilder`;
- `OutputValidationStrategy`;
- `AgentModelGateway`;
- `ToolRegistry`.

Это полезная платформа. Проблема не в agent runtime, а в том, что lesson layer пока не оформил полноценный контракт модуля поверх этих extension points.

## Как добавить новый модуль сегодня

Минимальный текущий контракт добавления нового модуля выглядит так:

1. Добавить строку в `lesson_modules`.
2. Добавить или переиспользовать `agent_profiles`.
3. Добавить `agent_pre_generation_workflows`, если нужен module-specific context.
4. Реализовать Java bean `LessonModuleStrategy`:
   - `moduleKey()`;
   - `validateDraftForGeneration(...)`;
   - `validateLesson(...)`;
   - `generationInstructions()`.
5. Если нужен multi-step generation, реализовать Java bean `LessonGenerationPipeline`.
6. Если pipeline имеет stage artifacts, написать stage validators.
7. Обновить fake model/test fixtures.
8. Добавить интеграционные тесты генерации и валидации.
9. Если фронтенд должен отображать тело урока, добавить renderer под новую структуру.

Этот контракт не закреплен в одном месте кода и не представлен как единая "module definition". Разработчику нужно знать несколько разрозненных мест: БД migrations, lesson strategy, agent profiles, pre-generation workflows, validation strategy, pipeline, frontend.

## Что уже сделано хорошо

### 1. Есть Strategy pattern для module-specific валидации

`LessonModuleStrategyCatalog` индексирует все `LessonModuleStrategy` по `moduleKey`. Это правильный Open/Closed подход: новый модуль может добавлять новый strategy bean без изменения каталога.

Сильная сторона: предметные правила `TestModule` и `hsk5_v1` уже разделены.

Ограничение: общий `LessonContentValidator` все равно навязывает единую top-level структуру до вызова strategy.

### 2. Есть разделение generic runtime и lesson domain

`AgentRuntimeOrchestrator` не знает про HSK5 напрямую. Он умеет запускать profile, строить context, применять pre-generation workflow, валидировать output и чинить invalid output. Это хорошая архитектурная граница.

Ограничение: `LessonGeneratedContentOutputValidationStrategy` уже является lesson-specific validator внутри runtime validation extension и содержит HSK5 composer special case.

### 3. Есть module-level prompt appendix

`LessonModule.systemPromptAppendix` позволяет менять инструкции на уровне модуля без изменения generic profile prompt.

Сильная сторона: структура урока частично задается системным промптом.

Ограничение: prompt не является надежным контрактом сам по себе. Контракт должен быть также машинно проверяемым: JSON Schema, Java validator или другой formal contract.

### 4. Есть pre-generation workflows

`agent_pre_generation_workflows` позволяет конфигурировать набор шагов до генерации. Это уже отвечает на часть вопроса "могу ли я поменять workflow в зависимости от модуля".

Сильная сторона: workflow variant можно выбирать через `generatorWorkflowVariantKey`.

Ограничение: это только pre-generation часть. Стадии самой генерации урока пока не описываются декларативно, кроме кастомного Java pipeline.

### 5. Есть generation trace

`lesson_generation_runs` и `lesson_generation_run_stages` дают базовую наблюдаемость multi-stage генерации.

Сильная сторона: можно видеть stage order, stage output, failure reason и final generator session.

Ограничение: trace схема пока не хранит contract version, prompt snapshot, input snapshot, stage duration как отдельные timestamps, retry/repair metadata на уровне stage.

### 6. Есть output repair

`FinalOutputValidationService` и repair loop в `AgentRuntimeOrchestrator` дают важную устойчивость к LLM output drift.

Сильная сторона: invalid JSON или schema mismatch может быть исправлен автоматически.

Ограничение: repair prompt строится из generic validation issues. Для сильно разных модулей лучше иметь module-aware repair guidance.

## Основные архитектурные проблемы

### 1. Общий lesson envelope слишком жесткий

`LessonContentValidator` требует:

- `schemaVersion`;
- `title`;
- `studyLanguage`;
- `explanationLanguage`;
- `translationLanguage`;
- `newWords`;
- `reviewWords` как optional fallback;
- `sections`.

Это нормально для текущих уроков, но не подходит для "абсолютно разных" структур. Например, будущий модуль может быть:

- диалоговым симулятором без `sections`;
- аудио-диктантом с `clips`, `transcript`, `rubric`;
- экзаменационным модулем с `tasks`, `answers`, `scoring`;
- story-based модулем с `chapters`, `characters`, `branching`;
- lesson plan с `phases`, `teacherScript`, `studentActions`;
- карточками без чтения и без top-level `newWords`.

Сейчас такие модули придется искусственно запихивать в `sections` и `newWords`, либо менять общий validator и рисковать сломать существующие модули.

Рекомендация: разделить stable platform envelope и module payload.

Пример целевой формы:

```json
{
  "moduleKey": "hsk5_v1",
  "schemaVersion": 1,
  "title": "Lesson title",
  "metadata": {
    "studyLanguage": "zh",
    "explanationLanguage": "ru",
    "translationLanguage": "ru"
  },
  "payload": {
    "sections": []
  }
}
```

Для радикально разных модулей обязательным должен быть только минимальный platform envelope. Все остальное должно жить в `payload` и проверяться module-specific контрактом.

### 2. Vocabulary tracking зашит как обязательная cross-cutting логика

`GeneratedLessonPersister` всегда вызывает:

- `lessonVocabularyTrackingService.recordLessonVocabulary(lesson, payload.newWords())`;
- `lessonVocabularyTrackingService.recordReviewedVocabulary(lesson, payload.reviewWords())`.

`ValidatedLessonPayload` всегда содержит `newWords` и `reviewWords`.

Это удобно для HSK/TestModule, но не все модули обязаны иметь словарь. Также разные модули могут извлекать словарь из разных мест: `payload.sections`, `payload.tasks`, `payload.cards`, `payload.dialogues`.

Рекомендация: вынести vocabulary extraction в отдельный module extension point.

Целевой интерфейс:

```java
public interface LessonVocabularyExtractor {
    String moduleKey();

    LessonVocabularyExtractionResult extract(JsonNode lessonJson);
}
```

Для модулей без словаря extractor возвращает пустой результат или module definition явно ставит `vocabularyPolicy: disabled`.

### 3. Контракт модуля раздвоен между prompt, Java и БД

Сейчас правила находятся в нескольких местах:

- `lesson_modules.system_prompt_appendix`;
- `LessonModuleStrategy.generationInstructions()`;
- `LessonModuleStrategy.validateLesson(...)`;
- `agent_profiles.output_contract_json`;
- HSK5 stage prompts в `Hsk5QualityLessonGenerationPipeline`;
- HSK5 artifact validators;
- integration tests/fake model.

Это создает риск drift: prompt просит одно, validator требует другое, output contract проверяет третье.

Рекомендация: создать single source of truth для module contract.

Практичные варианты:

1. JSON Schema как контракт `payload`.
2. Java `LessonModuleDefinition` как кодовый контракт, который отдает prompt instructions, validator, schema metadata и generation plan.
3. Гибрид: JSON Schema для shape, Java validators для semantic rules.

Лучший вариант для текущего Java/Spring проекта: гибрид. JSON Schema закрывает структуру, Java strategy закрывает семантику, prompt appendix генерируется или хотя бы тестируется против schema.

### 4. Multi-step generation не является модульно-декларативным

`Hsk5QualityLessonGenerationPipeline` жестко содержит:

- stage order;
- stage keys;
- agent profile keys;
- task text;
- stage input composition;
- prompt appendix per stage;
- HSK5 artifact validators;
- composer input composition.

Новый модуль с другим process creation сейчас требует новый Java pipeline. Это может быть нормально для сложной логики, но плохо как единственный способ масштабирования.

Рекомендация: разделить два уровня.

Уровень A: declarative generation plan для типовых pipeline:

```json
{
  "pipelineKey": "multi-stage-artifact-composer:v1",
  "stages": [
    {
      "stageKey": "blueprint",
      "profileKey": "lesson-stage:module_blueprint",
      "input": ["baseInput"],
      "outputArtifactKey": "blueprint",
      "validatorKey": "module-blueprint"
    },
    {
      "stageKey": "composer",
      "profileKey": "lesson-generator:module_composer",
      "input": ["baseInput", "blueprint"],
      "outputArtifactKey": "finalLesson",
      "validatorKey": "module-final"
    }
  ]
}
```

Уровень B: custom Java `LessonGenerationPipeline` только для нетиповых сценариев: branching, external tools, heavy post-processing, adaptive retries.

### 5. `LessonModuleStrategy` смешивает несколько ответственностей

Один интерфейс отвечает за:

- draft policy;
- final lesson validation;
- prompt instructions.

Для простых модулей это удобно. Для десятков модулей интерфейс станет слишком широким.

Рекомендация: разделить на более мелкие роли:

```java
public interface LessonDraftPolicy {
    void validate(LessonDraftView draft);
}

public interface LessonContentContract {
    ValidatedLessonPayload validate(JsonNode lessonJson, LessonModule module);
}

public interface LessonPromptContract {
    String systemInstructions(LessonModule module);
}

public interface LessonGenerationWorkflowDefinition {
    String workflowKey();
    LessonGenerationPlan plan();
}
```

Затем собрать их через `LessonModuleDefinition`.

### 6. Output validation strategy выбирается не по explicit key

Сейчас `OutputValidationStrategyCatalog` выбирает стратегии через `supports(...)`. Это гибко, но для большого числа модулей может стать неявным: стратегия может начать применяться к профилю только потому, что совпали prefix и required fields.

Существующий документ `backend/docs/agent-output-validation.md` отмечает, что `output_validation_strategy_key` оставлен как legacy, а selection идет автоматически. Это приемлемо для малого набора стратегий.

Для масштабируемой платформы модулей лучше сделать выбор валидации явным на уровне module contract или profile metadata:

- `validatorKeys` в `agent_profiles.output_contract_json`;
- `finalOutputValidatorKey` в `LessonModuleDefinition`;
- `stage.validatorKey` в generation plan.

Автоматическое `supports(...)` можно оставить как fallback, но не как основной контракт модулей.

### 7. Agent profile `model_key` есть в БД, но не является частью доменной модели

В `agent_profiles` хранится `model_key`, но `AgentProfile` в домене его не содержит, а `StartAgentSessionCommand` всегда получает model key снаружи.

Для модулей это значит:

- есть глобальный default model через `LessonGenerationProperties`;
- caller может передать `modelKey`;
- но module/profile default model фактически не используется как policy.

Если разные модули требуют разные модели, лучше сделать model resolution явным:

1. request modelKey;
2. module default model key;
3. profile default model key;
4. global default model key.

### 8. Frontend пока не готов к произвольной структуре уроков

Сейчас dashboard выводит список модулей и заголовки уроков. Полного renderer для lesson content почти нет. Это хорошо в том смысле, что backend может развиваться без immediate UI coupling.

Но если продукт должен показывать разные типы уроков, нужен отдельный frontend contract:

- module renderer registry;
- fallback JSON renderer для неизвестных модулей;
- rendering hints в module contract;
- versioned UI adapter per `moduleKey + schemaVersion`.

Без этого backend сможет хранить разные структуры, но frontend не сможет надежно их отображать.

### 9. Миграции стали основным способом конфигурации модулей

Модули, профили и workflow создаются через Flyway SQL. Это надежно для versioned deployment, но неудобно для частого добавления педагогических модулей.

Рекомендация:

- оставить Flyway для production baseline;
- добавить seed/config loader для module definitions в YAML/JSON;
- добавить admin/import command для новых модулей;
- добавить contract tests, которые проверяют, что каждый active module имеет strategy/schema/profile/workflow.

### 10. Есть признаки encoding/drift в seed данных и документах

В некоторых тестовых данных и frontend строках видна mojibake-кодировка. Это не блокирует архитектуру генерации, но опасно для prompt quality и пользовательского текста.

Рекомендация: зафиксировать UTF-8 policy для SQL migrations, frontend source и test fixtures. Для prompts это особенно важно, потому что битый текст напрямую ухудшает качество генерации.

## Ответы на ключевые вопросы

### Насколько архитектура готова к масштабированию?

Готова частично.

Хорошо готова к добавлению модулей, которые:

- используют один lesson JSON envelope;
- имеют `newWords`, `reviewWords`, `sections`;
- отличаются section types и prompt instructions;
- создаются через one-shot profile или один hardcoded Java pipeline.

Плохо готова к модулям, которые:

- не имеют `sections`;
- не имеют словаря;
- имеют другой top-level JSON;
- требуют branching workflow;
- требуют разных stage contracts;
- требуют разных post-processing/persistence policies;
- должны добавляться без Java-кода.

### Легко ли добавлять новые модули?

Сейчас добавление модуля средней сложности. Для простого модуля нужно не очень много кода, но контракт размазан по нескольким местам. Для сложного модуля нужно писать много boilerplate: strategy, SQL profiles, workflow, fake model, tests, возможно pipeline и validators.

Чтобы было легко, нужен единый checklist и единая module definition.

### Насколько логика создания урока подменяема?

Подменяема на двух уровнях:

- one-shot flow: через `generatorProfileKey`, `generatorWorkflowVariantKey`, prompt appendix и pre-generation workflow;
- multi-step flow: через `generationPipelineKey`.

Но степень подменяемости ограничена:

- pre-generation workflow декларативный, но generation stages нет;
- pipeline stage order и validators сейчас Java-код;
- module-specific workflow не хранится как единый generation plan;
- общий финальный validator все равно навязывает общий lesson envelope.

### Можно ли поменять workflow в зависимости от модуля?

Да, но не полностью удобно.

Сегодня можно:

- для one-shot модуля выбрать другой `generatorProfileKey`;
- выбрать другой `generatorWorkflowVariantKey`;
- указать `generationPipelineKey` и уйти в отдельный pipeline.

Но для нового multi-step workflow нужно писать новый Java `LessonGenerationPipeline` или расширять существующий механизм.

### Можно ли задавать новую структуру урока в системном промпте?

Можно, но этого недостаточно для надежной архитектуры.

Системный промпт должен быть инструкцией для модели, а не единственным источником истины. Архитектурный контракт должен быть проверяемым:

- JSON Schema или Java validator;
- tests/golden examples;
- output contract;
- repair guidance;
- persistence/extraction policy.

Если оставить контракт только в prompt, система будет принимать invalid lessons, ломать frontend, терять словарь или создавать несовместимые версии после изменения prompt.

## Целевой архитектурный контракт нового модуля

Рекомендуемый контракт нового lesson module:

```java
public interface LessonModuleDefinition {
    String moduleKey();

    int schemaVersion();

    LessonDraftPolicy draftPolicy();

    LessonPromptContract promptContract();

    LessonContentContract contentContract();

    LessonGenerationDefinition generationDefinition();

    LessonVocabularyPolicy vocabularyPolicy();

    LessonRenderingContract renderingContract();
}
```

Где:

- `LessonDraftPolicy` проверяет, какие источники draft разрешены.
- `LessonPromptContract` строит module-specific system instructions.
- `LessonContentContract` валидирует payload и semantic rules.
- `LessonGenerationDefinition` описывает one-shot или multi-stage workflow.
- `LessonVocabularyPolicy` говорит, нужно ли извлекать новые/review слова и откуда.
- `LessonRenderingContract` дает frontend hints или renderer key.

Для текущей архитектуры можно внедрять это постепенно, не ломая существующий код.

## Рекомендуемая целевая модель данных

### `lesson_modules`

Добавить или концептуально закрепить:

- `module_key`;
- `schema_version`;
- `display_name`;
- `status`;
- `contract_json`;
- `generation_definition_json`;
- `renderer_key`;
- `default_model_key`;
- `created_at`;
- `updated_at`.

`system_prompt_appendix`, `generator_profile_key`, `generator_workflow_variant_key`, `generation_pipeline_key` можно оставить как compatibility layer.

### `contract_json`

Пример:

```json
{
  "envelopeVersion": 1,
  "payloadSchemaRef": "classpath:/lesson-modules/hsk5_v1/schema-v1.json",
  "requiredMetadata": ["studyLanguage", "explanationLanguage", "translationLanguage"],
  "vocabularyPolicy": {
    "enabled": true,
    "newWordsPath": "$.payload.newWords",
    "reviewWordsPath": "$.payload.reviewWords"
  },
  "renderer": {
    "key": "lesson-sections:v1",
    "supportedSectionTypes": ["word_study", "grammar", "conversation", "text", "word_game"]
  }
}
```

### `generation_definition_json`

Пример one-shot:

```json
{
  "type": "one-shot",
  "profileKey": "lesson-generator:test-module-v1",
  "workflowVariantKey": "draft-generation-with-review:v1",
  "finalValidatorKey": "lesson-module:test-module-v1"
}
```

Пример multi-stage:

```json
{
  "type": "multi-stage",
  "pipelineKey": "artifact-composer:v1",
  "stages": [
    {
      "stageKey": "blueprint",
      "profileKey": "lesson-stage:hsk5_v1_blueprint",
      "promptTemplateKey": "hsk5-blueprint:v1",
      "inputArtifactKeys": ["baseInput"],
      "outputArtifactKey": "blueprint",
      "validatorKey": "hsk5-blueprint:v1"
    },
    {
      "stageKey": "composer",
      "profileKey": "lesson-generator:hsk5_v1_composer",
      "promptTemplateKey": "hsk5-composer:v1",
      "inputArtifactKeys": ["baseInput", "blueprint"],
      "outputArtifactKey": "finalLesson",
      "validatorKey": "hsk5-final:v1"
    }
  ]
}
```

## Целевая схема generation workflow

Рекомендуемый поток:

1. `LessonApplicationService` получает `moduleKey`.
2. `LessonModuleDefinitionCatalog` возвращает definition.
3. `definition.draftPolicy().validate(draft)`.
4. `LessonGenerationOrchestrator` запускает `definition.generationDefinition()`.
5. Каждый stage получает:
   - immutable base input;
   - explicit artifact inputs;
   - prompt contract;
   - output contract;
   - validator key.
6. Stage output сохраняется в trace вместе с contract snapshot.
7. Final output валидируется через `definition.contentContract()`.
8. `LessonPersister` сохраняет generic lesson envelope и raw payload.
9. `LessonVocabularyExtractor` запускается только если включен vocabulary policy.
10. Событие `LessonGenerated` публикуется для дальнейших async процессов.

Такой flow позволяет иметь и простые, и сложные модули без изменения `LessonApplicationService`.

## Рекомендуемые изменения по приоритетам

### P0 - закрепить текущий контракт в коде и тестах

1. Создать `LessonModuleDefinition` или хотя бы `LessonModuleContract` как facade над текущими `LessonModuleStrategy`.
2. Добавить тест "каждый active lesson_module имеет strategy".
3. Добавить тест "каждый module strategy имеет matching DB module seed".
4. Добавить тест "каждый module generator profile существует".
5. Добавить тест "generationPipelineKey существует в `LessonGenerationPipelineCatalog`, если задан".
6. Описать в docs текущий checklist добавления модуля.

### P1 - отделить platform envelope от module payload

1. Ввести новый `ValidatedLessonPayload`, который хранит:
   - platform metadata;
   - raw `JsonNode payload`;
   - optional vocabulary extraction result.
2. Перестать требовать `newWords` и `sections` на общем уровне.
3. Перенести проверку `sections/newWords/reviewWords` в module-specific contracts.
4. Сохранить backward compatibility для текущих `TestModule` и `hsk5_v1`.

### P2 - вынести vocabulary extraction из общего persister

1. Создать `LessonVocabularyExtractor` registry.
2. Для `TestModule` и `hsk5_v1` реализовать extractor.
3. Для модулей без словаря вернуть empty extraction.
4. Убрать жесткую зависимость `GeneratedLessonPersister` от `ValidatedLessonPayload.newWords/reviewWords`.

### P3 - сделать declarative pipeline engine

1. Создать generic `ArtifactComposerLessonGenerationPipeline`.
2. Хранить stage plan в `generation_definition_json` или отдельной таблице.
3. Дать каждому stage:
   - profileKey;
   - prompt template;
   - input artifact mapping;
   - validatorKey;
   - retry policy.
4. Оставить Java custom pipeline только как escape hatch.

### P4 - contract-first validation

1. Добавить JSON Schema support для module payload.
2. Java validators оставить для semantic rules.
3. Добавить golden fixture tests:
   - valid minimal lesson;
   - valid rich lesson;
   - invalid examples per rule.
4. Сохранять contract version/snapshot в generation trace.

### P5 - frontend renderer contract

1. Добавить `rendererKey` в module summary API.
2. Создать frontend registry `moduleKey + schemaVersion -> renderer`.
3. Добавить fallback renderer для неизвестных модулей.
4. Для section-based модулей сделать общий renderer по `section.type`.

## Definition of Ready для нового модуля

Новый модуль считается готовым к добавлению, если есть:

- `moduleKey` и `schemaVersion`;
- формальный lesson payload contract;
- system prompt appendix или prompt template;
- draft input policy;
- generation definition: one-shot или multi-stage;
- final output validator;
- vocabulary policy: enabled/disabled/extractor;
- rendering contract или explicit backend-only статус;
- seed/config для agent profile;
- seed/config для pre-generation workflow;
- fake model fixture;
- unit tests валидатора;
- integration test генерации;
- migration/config test, что active module полностью связан.

## Definition of Done для архитектуры масштабируемых модулей

Архитектура готова к "абсолютно разным урокам", когда:

- `LessonApplicationService` не меняется при добавлении обычного нового модуля;
- новый one-shot модуль добавляется через module definition + validator + profile config;
- новый типовой multi-stage модуль добавляется через declarative generation plan;
- custom Java pipeline нужен только для действительно нетиповой логики;
- общий validator не требует `sections` и `newWords`;
- vocabulary tracking является optional policy;
- output validation выбирается явно по module/profile/stage contract;
- frontend понимает renderer contract;
- есть contract tests на полноту регистрации всех active modules.

## Риски, если ничего не менять

1. Каждый новый модуль будет увеличивать количество HSK/TestModule-specific условий.
2. Prompt и validator будут расходиться.
3. Добавление модулей станет дорогостоящим и рискованным.
4. Уроки с другой top-level структурой придется искусственно адаптировать под `sections`.
5. Vocabulary tracking будет ломаться или сохранять нерелевантные данные.
6. Multi-stage workflows превратятся в набор hardcoded Java классов.
7. Frontend не сможет надежно отображать разные lesson payloads.
8. Сложнее будет версионировать модули и мигрировать старые уроки.

## Практический план внедрения без большой переписки ядра

### Шаг 1

Оставить существующий `LessonModuleStrategy`, но добавить рядом `LessonModuleContractRegistry`, который проверяет полноту регистрации.

### Шаг 2

Ввести `LessonEnvelopeValidator`, который проверяет только:

- root JSON object;
- `moduleKey`;
- `schemaVersion`;
- `title` или module-specific title extractor;
- optional metadata.

Все остальное перенести в `LessonModuleStrategy.validateLesson`.

### Шаг 3

Переименовать текущий `LessonModuleStrategy` в более явный составной контракт или обернуть:

```java
public record LessonModuleRuntimeContract(
        LessonDraftPolicy draftPolicy,
        LessonPromptContract promptContract,
        LessonContentContract contentContract,
        LessonVocabularyExtractor vocabularyExtractor,
        LessonGenerationDefinition generationDefinition) {}
```

### Шаг 4

Сделать `GeneratedLessonPersister` независимым от словаря. После сохранения урока отдельно вызвать optional extractor.

### Шаг 5

Сделать generic multi-stage pipeline для artifact composer. Перенести HSK5 stage order из `Hsk5QualityLessonGenerationPipeline` в declarative plan, а HSK5 Java-класс оставить как временный adapter или удалить после миграции.

### Шаг 6

Добавить tests на масштабируемость:

- dummy module без `sections`;
- dummy module без vocabulary;
- dummy module с multi-stage plan;
- проверка, что генерация не требует изменения `LessonApplicationService`.

## Итоговое архитектурное решение

Текущая архитектура подходит как MVP для нескольких lesson modules, но еще не является платформой модулей. Главная доработка: сделать модуль урока first-class contract, а не совокупность SQL rows, Java strategy и prompt текста.

Рекомендуемая целевая архитектура:

- stable lesson envelope в core;
- module payload полностью принадлежит модулю;
- module definition как единый контракт;
- prompt является производной/частью контракта, но не единственным контрактом;
- generation workflow выбирается из module definition;
- типовые workflows декларативны;
- сложные workflows расширяются через Java pipeline;
- vocabulary и rendering являются optional module policies;
- tests проверяют полноту регистрации и совместимость contract/prompt/validator.

После этих изменений проект сможет безопасно поддерживать разные уроки, разные структуры и разные процессы создания без разрастания центральной логики.
