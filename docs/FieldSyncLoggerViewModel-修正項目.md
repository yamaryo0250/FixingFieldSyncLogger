# FieldSyncLoggerViewModel 修正項目

対象は `FieldSyncLoggerViewModel` と `FieldSyncLoggerViewModelTest`。画面の分岐は変えない。`MainActivity` は、ボタン無効化にすでに `isLoading` を使っている。

`isLoading` は保存中だけを表す。取得の開始、最初の一覧、取得失敗では変えない。これは現状のままで、今回の修正に入れない。

失敗後の自動再取得と、購読が 5 秒途切れたあとにエラーを消す処理も、今回入れない。

## 実ロジック

### コメントを実装に合わせる

取得失敗の `catch` にある「Flowが終了しないように」は、実装と食い違う。`emit(emptyList())` のあと、その収集は正常完了する。コメントをこの動きに合わせる。

失敗後も Flow を開き続ける実装にはしない。失敗後の再取得は足さない。`_hasLoadError` を、その後の成功で `false` に戻す処理も足さない。

`onSaveAndSyncClicked` の KDoc と、`MainActivity.updateSaveButtonEnabled` のコメントは、いまどおり未入力と保存中の話なので、取得中は足さない。

### 保存処理のキャンセルは、保存失敗にしない

`catch (e: Exception)` は `CancellationException` も捕まえる。画面破棄で `viewModelScope` がキャンセルされると、DB 保存失敗と同じ `SaveFailure` を送る経路に入る。

キャンセルは `LoggerEvent.SaveFailure` にせず、キャンセルのまま戻す。`IOException` などの保存失敗は、いまどおり `LoggerEvent.SaveFailure(R.string.msg_save_error)` を送る。

保存の `isLoading` は、いまどおり開始で `true`、成功でも失敗でも終わる `finally` で `false` に戻す。

## テストコード

`FieldSyncLoggerViewModelTest` を、上の契約に合わせる。UseCase はモックのままにする。`loadReportsUseCase()` が 2 回呼ばれることは期待値にしない。

取得の初期値、成功、失敗で、`isLoading` を `true` に変えた期待値にはしない。取得中は `false` のままである。

### 保存の観測

- `isLoading` の `true` から `false` は、`uiState` を購読したまま見る。正常系は、購読を終えてから保存している
- 異常系は、失敗を起こす前から `uiEvent` を購読し、その購読で `LoggerEvent.SaveFailure(R.string.msg_save_error)` を 1 件受け取る。保存完了後に購読して、チャネルに残った値を見ている状態をやめる
- 正常系は、完了まで `SaveFailure` が無いことまで見る
- `saveLoggerUseCase(any())` をやめる。渡した文字列と、呼び出し 1 回を見る。正常系・異常系とも、戻り値は開始できたので `true`

### 拒否と再送

- 空文字、空白だけの文字列は `false`。UseCase は呼ばれない
- 前後に空白がある非空白の文字列は、その文字列のまま 1 回渡る
- 保存が完了する前の 2 回目は `false`。UseCase は 1 回のまま
- 保存失敗で `isLoading` が `false` に戻ったあと、同じ内容をもう一度押すと `true` になり、UseCase が再びその文字列で呼ばれる

### 同じ購読の 2 件目

1 回の `loadReportsUseCase()` が返した Flow が、リスト 1 のあとリスト 2 を流す。購読を維持したまま、`uiState.reports` がリスト 2 になる。保存中でなければ、`isLoading` は `false` のままである。

これは `loadReportsUseCase()` の 2 回目の呼び出しではない。保存や同期で `reports` テーブルが変わると、開いたままの画面へ次の全件が届く。ViewModel はそのリストで `reports` を置き換える。

保存中にその次の一覧が届いたとき、`reports` は新しいリストになり、`isLoading` は `true` のままにする。保存完了ではじめて `false` に戻る。一覧の更新で、保存中の `isLoading` を下ろさない。

### キャンセル

保存処理が `CancellationException` でキャンセルされたとき、`SaveFailure` は流れない。

## 今回入れないもの

- 取得中の `isLoading` 管理。取得の開始、最初の一覧、取得失敗で `isLoading` を変えない
- 取得中のクリックを、保存中と同じ理由で拒否すること
- 取得失敗後の自動再取得
- 購読が 5 秒途切れたあとの集め直しで、`_hasLoadError` を戻してエラー表示を消すこと
- `loadReportsUseCase()` が複数回呼ばれること
