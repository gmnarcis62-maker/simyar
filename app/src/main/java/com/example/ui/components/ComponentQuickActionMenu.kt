package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CircuitComponent

@Composable
fun ComponentQuickActionMenu(
    component: CircuitComponent,
    onCopy: () -> Unit,
    onRotate: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onShowSpecs: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier.padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${component.label} - ${component.type.persianName}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ردیف آیکون‌های عملکرد سریع (Outlined Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "کپی", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("کپی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                IconButton(onClick = onRotate, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.RotateRight, contentDescription = "چرخش", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Text("چرخش", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                IconButton(onClick = onToggleLock, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (component.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "قفل",
                            tint = if (component.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(if (component.isLocked) "بازکردن" else "قفل", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                IconButton(onClick = onShowSpecs, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Info, contentDescription = "مشخصات", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Text("فنی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("حذف", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
