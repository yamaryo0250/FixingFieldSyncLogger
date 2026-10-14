# StateFlow と Turbine で ViewModel テストに詰まった記録

Android の ViewModel テストを、Turbine と `kotlinx-coroutines-test` で書いていて止まった箇所のメモです。失敗の見た目は `uiState` が空のまま、Turbine の 3 秒タイムアウト、`AssertionError` のどれかでした。原因はモックの戻り値そのものではなく、購読、スケジューラ、例外の発生場所、見ている Flow の違いにありました。

想定読者は、`stateIn` と `runTest` を使い始めた人です。ライブラリの版は次のとおりです。

- `kotlinx-coroutines-test` 1.7.3
- Turbine 1.1.0
- MockK 1.13.8

## 対象の ViewModel

一覧は `SharingStarted.WhileSubscribed(5_000)` で `StateFlow` にしています。購読者がいる間だけ上流を動かします。購読前の値は `initialValue` の空の `MainUiState` です。

```kotlin
val uiState: StateFlow<MainUiState> = combine(
    reportsState,
    _isLoading,
    _hasLoadError
) { state, isLoading, hasLoadError ->
    MainUiState(
        reports = state,
        isLoading = isLoading,
        errorMessage = if (hasLoadError) R.string.msg_load_error else null
    )
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = MainUiState(),
)
```

取得失敗は、UseCase が返した `Flow` の `catch` で受けます。

```kotlin
private val reportsState: Flow<List<Report>> = loadReportsUseCase()
    .catch { e ->
        Log.e("FieldSyncLoggerViewModel", "Failed to load reports", e)
        _hasLoadError.value = true
        emit(emptyList())
    }
```

保存の失敗は `uiState` ではなく、別の `uiEvent` へ送ります。

```kotlin
private val _uiEvent = Channel<LoggerEvent>(Channel.BUFFERED)
val uiEvent: Flow<LoggerEvent> = _uiEvent.receiveAsFlow()

fun onSaveAndSyncClicked(content: String): Boolean {
    if (_isLoading.value || content.isBlank()) {
        return false
    }

    _isLoading.value = true
    viewModelScope.launch {
        try {
            saveLoggerUseCase(content)
        } catch (e: Exception) {
            Log.e("FieldSyncLoggerViewModel", "Save logger failed.", e)
            _uiEvent.send(LoggerEvent.SaveFailure(R.string.msg_save_error))
        } finally {
            _isLoading.value = false
        }
    }
    return true
}
```

本番で `uiState` を購読しているのは画面です。`MainActivity` が `STARTED` の間 `uiState.collect` しています。この `collect` が、テストにおける購読者に相当します。

`viewModelScope` は `Dispatchers.Main` を使います。テストでは `StandardTestDispatcher` を 1 つ作り、`Dispatchers.setMain` と `runTest` の両方へ渡します。`viewModelScope` は、最初に使われた時点の `Main` を掴むので、ViewModel を作る前に差し替えます。

```kotlin
private val dispatcher = StandardTestDispatcher()

@Before
fun setUp() {
    Dispatchers.setMain(dispatcher)
}

@Test
fun example() = runTest(dispatcher) {
    // この中で ViewModel を作る
}
```

`UnconfinedTestDispatcher()` を引数なしで別に作ると、専用のスケジューラがもう 1 つできます。`runTest` の `advanceUntilIdle()` は、自分のスケジューラしか進めません。

## 1. 取得成功の期待値が、空リストの初期値だった

失敗は次の形でした。

```text
Expected :MainUiState(reports=[...], isLoading=false, errorMessage=null)
Actual   :MainUiState(reports=[], isLoading=false, errorMessage=null)
```

スタブは `flowOf(reports)` です。`advanceUntilIdle()` も呼んでいました。それでも Actual は `initialValue` のままです。

`WhileSubscribed` は、購読者が現れてから `loadReportsUseCase()` と `combine` を開始します。このテストには購読者がいませんでした。

- `uiState.value` は現在値を読むだけで、購読にならない
- `advanceUntilIdle()` は、すでに積まれたコルーチンを進めるだけで、購読者を作らない

そのため UseCase は呼ばれず、期待したリストは `combine` に乗りません。初期状態のテストが空リストで成功していても、UseCase が空リストを流したことにはなりません。あのテストが見ているのも、購読前の `initialValue` です。

本番の `WhileSubscribed` を、テストを通すために `Eagerly` へ変えるのは、この失敗の直し方ではありません。画面が購読していない間は上流を止める、という実装をテスト側が再現します。

購読後の観測は、Turbine だと次の順になります。

```kotlin
viewModel.uiState.test {
    awaitItem() // 1件目は MainUiState()。上流はまだ動いていない
    advanceUntilIdle()
    awaitItem() // 2件目が UseCase の結果
}
```

`StateFlow` は、購読開始時に現在値をすぐ再送します。`first()` のように 1 件取って購読を終えると、2 件目の前に購読が終わり、また空リストになります。

`UnconfinedTestDispatcher` のまま購読だけ足すと、この 2 件が分かれないことがあります。`Unconfined` はその場で最後まで実行し、`flowOf` には待ちがありません。購読者が最初の値を読む前に値が更新され、`StateFlow` が途中値を畳み込みます。テストが突然成功しても、「1 件目は初期値」は観察できていません。順序を見るなら `StandardTestDispatcher` です。指示があるまで上流を実行しないので、1 件目のあと `advanceUntilIdle()` すると 2 件目が来ます。

もう 1 つ、モックの API です。今回の Actual はモック不一致の例外ではないので、先に直すのは未呼び出しの方です。`LoadReportsUseCase.invoke()` は `suspend` ではありません。通常の関数は `every { loadReportsUseCase() } returns flowOf(reports)` です。`coEvery` は `suspend` 用です。購読を足したあとに MockK の `no answer found` が出るなら、スタブの種類を確認します。例外にならずリストが入るなら、その版の MockK は `coEvery` も通常関数のスタブとして使えています。どちらになるかは、購読後の実行まで分かりません。

取得テストの購読を `setUp` に共通化すると、初期状態テストの意味も変わります。購読を始めるタイミングはテストごとに分けます。

## 2. 取得失敗が `Flow.catch` に入らない

次のスタブで、テストは `Exception: TEST` のまま落ちました。

```kotlin
every { loadReportsUseCase() } throws Exception("TEST")
```

疑っていたのは、「`catch` が受け取れるのは Flow 用の例外だけで、この `Exception` は対象外ではないか」です。型の問題ではありません。例外の位置が、`catch` を付けるより前です。

`reportsState` の初期化は次の順です。

1. `loadReportsUseCase()` を呼ぶ。戻り値として `Flow` が必要
2. 返ってきた `Flow` に `.catch` を付ける

`throws` は 1 で落ちます。`Flow` が返らないので 2 は実行されず、`catch` は一度も付きません。この初期化はコンストラクタの中なので、`awaitItem()` には到達しません。

スタックの先頭が `Exception("TEST")` を書いた行になるのは、スタックが生成箇所で固定されるためです。ViewModel のフレームが無いことは、「コンストラクタから飛んでいない」証拠にはなりません。

`catch` が受け取るのは、その上流を収集している間に、上流が失敗として流した例外です。`CancellationException` は受け取りません。下流の `collect` 側の例外も受け取りません。

本番の取得は、呼び出し時点では失敗しません。Room の `Flow` を返すだけです。問い合わせの失敗は、画面が購読して収集が始まってから起きます。`catch` が書かれているのは、その失敗に対してです。

スタブは、関数呼び出しを失敗させるのではなく、収集が始まってから失敗する `Flow` を返します。

```kotlin
every { loadReportsUseCase() } returns flow { throw Exception("TEST") }
```

`Flow` 自体はすぐ返り、中の `throw` は購読後の `advanceUntilIdle()` で起きます。そこで `catch` に入り、空リストと取得エラーが `uiState` に乗った値が 2 件目です。

## 3. `Log.e` をモックしたのに `not mocked` になる

`flow { throw ... }` のあとは、別の失敗になりました。

```text
No value produced in 3s
app.cash.turbine.TurbineAssertionError: No value produced in 3s
Suppressed: java.lang.RuntimeException: Method e in android.util.Log not mocked
    at FieldSyncLoggerViewModel.catch (Log.e の行)
```

`catch` には入っています。2 件目が来ないのは、その中の `Log.e` が再び落ちて、`emit(emptyList())` まで進んでいないためです。Turbine の 3 秒は、2 件目を待って諦めた時間です。原因は `Suppressed` にあります。

本番が呼んでいるのは 3 引数です。

```kotlin
Log.e("FieldSyncLoggerViewModel", "Failed to load reports", e)
```

スタブが 2 引数だけだと、別メソッドです。

```kotlin
mockkStatic(Log::class)
every { Log.e(any(), any()) } returns 0
```

一致するスタブが無いと、MockK は元のメソッドを呼びます。スタックの `SpyKStub.defaultAnswer` と `handleOriginalCall` がそれです。ユニットテストの `android.util.Log` には実体がないので、`Method e in android.util.Log not mocked` になります。

この例外は `catch` のラムダの中です。同じ `catch` は、自分のラムダで起きた例外を捕まえません。`_hasLoadError` は立たず、空リストも流れず、`uiState` は初期値のままです。

スタブは、本番と同じ 3 引数にします。`Log.e` は `Int` を返すので、`returns 0` のままで足ります。

```kotlin
every { Log.e(any(), any(), any()) } returns 0
```

タイムアウトのときは、自分のテストのフレームを先に見ます。`Suppressed` に例外があれば、emit の前で落ちています。無いなら、Flow は生きていて、単に新しい値がありません。

## 4. 空リストの取得では、2 件目の `awaitItem()` が来ない

保存の正常系でも、準備の 2 件目で `No value produced in 3s` になりました。`Suppressed` はありません。失敗地点は `onSaveAndSyncClicked` より前です。

`StateFlow` は、いまの値と `equals` で同じ値を再送しません。

1 件目は `initialValue` の `MainUiState()` です。空リスト、`isLoading = false`、`errorMessage = null` です。その後の `flowOf(emptyList())` が作る値も同じです。上流は動きますが、購読者への 2 件目はありません。取得成功のテストで 2 件目が来たのは、リストの中身が初期値と違ったからです。

この準備では 2 件目を待ちません。`advanceUntilIdle()` のあと、`uiState.value` が初期値のままであることを見れば、空リストの取得は確認できます。追加の値が無いことは `expectNoEvents()` でも見られます。

確認するなら、1 件目を変数に取り、`advanceUntilIdle()` のあとで `uiState.value` と比べます。同じなら、その `awaitItem()` は成功しません。

## 5. `isLoading = true` は、`runCurrent()` だけでは残らない

`isLoading = true` は、保存コルーチンが終わるまでの途中状態です。`StandardTestDispatcher` では、`_isLoading.value = true` の直後に `uiState` が変わるわけではありません。`combine` の再計算は行列に積まれます。クリック直後の `uiState.value` は、まだ `isLoading = false` です。

`advanceUntilIdle()` は、その再計算と、保存コルーチンの `finally` までまとめて実行します。UseCase がすぐ終わるスタブだと、読み取る頃には `isLoading` はすでに `false` です。

`runCurrent()` も、「`true` のところで止める」処理ではありません。仮想時刻を進めずに、今積まれている仕事を空になるまで実行します。途中で積まれた仕事も、中断せずに終わるなら同じ回で実行します。

スタブの無い `mockk()` は、呼び出されたときに `MockKException` を投げます。これは `Exception` なので `catch` に入り、`finally` まで続きます。`runCurrent()` の中は次の順です。

1. `combine` が動き、`uiState` がいったん `isLoading = true` になる
2. `launch` が動き、スタブの無い保存が例外を投げる
3. `finally` が `_isLoading` を `false` にする
4. その更新も同じ `runCurrent()` が処理する

戻ったときの値は `false` です。この経路は正常系でもありません。`coEvery { saveLoggerUseCase(any()) } returns Unit` でも、関数がすぐ終わるなら `finally` まで同じ回に入ります。

`true` を残すには、保存処理がテストの指示まで中断する必要があります。中断中は `finally` に届かないので、`runCurrent()` は再計算のあとで行列が空になり、`true` のまま戻ります。

```kotlin
val saveFinished = CompletableDeferred<Unit>()
coEvery { saveLoggerUseCase(any()) } coAnswers {
    saveFinished.await()
}
```

`await()` が中断点です。`returns` では中断できません。並びは次です。

1. このスタブを、クリックより前に置く
2. `onSaveAndSyncClicked("TEST")` を呼ぶ。ここではまだ `uiState` を比較しない
3. `runCurrent()` を呼ぶ。`isLoading = true` になり、保存は `await()` で止まったままになる
4. `isLoading = true` と `coVerify` を確認する。呼び出しは `await()` の前に記録されている
5. `saveFinished.complete(Unit)` で中断を解く
6. `advanceUntilIdle()` のあと、`isLoading = false` を確認する

`coVerify` を `runCurrent()` より前に置くと、`launch` がまだ動いていないので未呼び出しです。断言が `Expected isLoading=true / Actual isLoading=false` なら、`finally` がすでに走っています。

## 6. `SaveFailure` を、`uiState` の `awaitItem()` で待っていた

異常系の失敗は次の形でした。

```text
Expected :SaveFailure(message=2131689657)
Actual   :MainUiState(reports=[], isLoading=true, errorMessage=null)
```

`2131689657` は `R.string.msg_save_error` のリソース ID です。ID の取り違えではありません。`viewModel.uiState.test { }` の中の `awaitItem()` は、`uiState` の次の値です。`SaveFailure` は `uiEvent` にしか流れません。

`isLoading = true` を `uiState.value` で読んでも、Turbine の未読キューは減りません。`runCurrent()` で流れた `MainUiState(isLoading=true)` が残っていて、次の `awaitItem()` はそれを返します。Expected と Actual の型が違うときは、`awaitItem()` のレシーバがどの Flow かを見ます。

失敗イベント用の `awaitItem()` は、`uiEvent` を購読している側に置きます。その購読は、`advanceUntilIdle()` で `send` が走る前に始めます。

スタブの最後の行にも別の穴がありました。

```kotlin
coEvery { saveLoggerUseCase(any()) } coAnswers {
    delay(1000.milliseconds)
    IOException("TEST")
}
```

これは `IOException` を作っているだけで、投げていません。`invoke` は正常に終わり、`catch` は動かず、`SaveFailure` は送られません。`delay` のあとに `throw IOException("TEST")` して、初めて失敗側に入ります。

`uiState` の `awaitItem()` で取るのは、`isLoading` の変化です。失敗イベントと混ぜません。

## 失敗の見分け

| 見た目 | 先に見るもの | 意味 |
| --- | --- | --- |
| Actual が空の `MainUiState` | 購読の有無 | 上流がまだ動いていない初期値 |
| `No value produced in 3s` と `Suppressed` | `Suppressed` の先頭 | emit の前に別の例外で落ちている |
| `No value produced in 3s` だけで `Suppressed` が無い | 1 件目と `uiState.value` の比較 | 値は同じで、再送されていない |
| Expected と Actual の型が違う | `awaitItem()` のレシーバ | 別の Flow の未読値を取っている |

スタックは下から追わず、自分のテストファイルの一番上のフレームを見ます。`$1$2` のような名前は、テストメソッドの中の何番目のラムダかです。
