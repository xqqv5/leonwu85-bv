package dev.aaa1115910.bv.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.bv.repository.RepostPublicationStatus
import dev.aaa1115910.bv.viewmodel.RepostPublishViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RepostPublishDialog(
    item: DynamicItem,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: RepostPublishViewModel = koinViewModel(key = "repost-${item.id}")
) {
    val publication by viewModel.state.collectAsState()
    LaunchedEffect(item.id) { viewModel.prepare(item.commentId > 0 && item.commentType > 0) }
    var successHandled by remember { mutableStateOf(false) }
    LaunchedEffect(publication.status) {
        if (publication.status == RepostPublicationStatus.Success && !successHandled) {
            successHandled = true
            viewModel.acknowledgeSuccess()
            onSuccess()
        }
    }
    AlertDialog(
        onDismissRequest = { if (!publication.publishing) onDismiss() },
        title = { Text("转发动态") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.text, onValueChange = viewModel::updateText,
                    enabled = !publication.locksDraft, modifier = Modifier.fillMaxWidth(),
                    label = { Text("说点什么（选填）") }, minLines = 2, maxLines = 5
                )
                if (item.commentId > 0 && item.commentType > 0) {
                    Row {
                        Checkbox(checked = viewModel.alsoComment, onCheckedChange = viewModel::updateAlsoComment,
                            enabled = !publication.locksDraft)
                        Text("同时评论原动态", modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (publication.publishing) LinearProgressIndicator(Modifier.fillMaxWidth())
                viewModel.validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                publication.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (publication.status == RepostPublicationStatus.PartialSuccess) {
                    Text("重试只发送评论，已成功的转发会保留。", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.publish(checkNotNull(item.id), item.commentId, item.commentType) },
                enabled = !publication.publishing && (!viewModel.alsoComment || viewModel.text.isNotBlank())
            ) {
                Text(when (publication.status) {
                    RepostPublicationStatus.PartialSuccess -> "重试评论"
                    RepostPublicationStatus.Failed -> "重试转发"
                    else -> "发布"
                })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !publication.publishing) { Text("关闭") } }
    )
}
