package vn.edu.vhu.ltdd.a7_241a310088;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "A7_241A310088";

    private static final String CHANNEL_ID = "a7_channel";
    private static final int NOTI_ID = 1001;

    private ImageView imgAnh;
    private TextView tvTrangThai;

    // 1. Bộ xin quyền CAMERA
    private final ActivityResultLauncher<String> xinQuyenCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    moCamera();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    hienGiaiThich();
                } else {
                    hienMoCaiDat();
                }
            });

    // 2. Bộ xin quyền POST_NOTIFICATIONS
    private final ActivityResultLauncher<String> xinQuyenThongBao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    guiThongBao();
                } else {
                    Toast.makeText(this, "Bạn đã từ chối quyền gửi thông báo.", Toast.LENGTH_SHORT).show();
                }
            });

    // 3. Bộ chụp ảnh xem trước
    private final ActivityResultLauncher<Void> chupAnh = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            (Bitmap bitmap) -> {
                if (bitmap != null) {
                    imgAnh.setImageBitmap(bitmap);
                    tvTrangThai.setText("Ảnh xem trước: " + bitmap.getWidth() + " x " + bitmap.getHeight() + " pixel");
                } else {
                    Toast.makeText(this, "Đã hủy chụp ảnh.", Toast.LENGTH_SHORT).show();
                }
            });

    // 4. Bài nâng cao 1: Xin nhiều quyền cùng lúc (Camera + Thông báo)
    private final ActivityResultLauncher<String[]> xinNhieuQuyen = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean cameraGranted = result.get(Manifest.permission.CAMERA);
                Boolean notifGranted = result.get(Manifest.permission.POST_NOTIFICATIONS);

                StringBuilder sb = new StringBuilder("Kết quả xin nhiều quyền:\n");
                sb.append("- Camera: ").append(cameraGranted != null && cameraGranted ? "ĐÃ CẤP" : "TỪ CHỐI").append("\n");
                sb.append("- Thông báo: ").append(notifGranted != null && notifGranted ? "ĐÃ CẤP" : "TỪ CHỐI");

                Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
                capNhatTrangThai();
            });

    // 5. Bài nâng cao 2: Xin quyền đọc danh bạ
    private final ActivityResultLauncher<String> xinQuyenDanhBa = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    docVaHienThiDanhBa();
                } else {
                    Toast.makeText(this, "Bạn đã từ chối quyền đọc danh bạ!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        imgAnh = findViewById(R.id.imgAnh);
        tvTrangThai = findViewById(R.id.tvTrangThai);
        Button btnChupAnh = findViewById(R.id.btnChupAnh);
        Button btnThongBao = findViewById(R.id.btnThongBao);
        Button btnCaiDat = findViewById(R.id.btnCaiDat);
        Button btnXinNhieuQuyen = findViewById(R.id.btnXinNhieuQuyen);
        Button btnDocDanhBa = findViewById(R.id.btnDocDanhBa);

        taoKenhThongBao();

        btnChupAnh.setOnClickListener(v -> kiemTraRoiChup());
        btnThongBao.setOnClickListener(v -> kiemTraRoiGuiThongBao());
        btnCaiDat.setOnClickListener(v -> moCaiDatUngDung());

        // Sự kiện nút Bài nâng cao 1
        btnXinNhieuQuyen.setOnClickListener(v -> {
            String[] perms = { Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS };
            xinNhieuQuyen.launch(perms);
        });

        // Sự kiện nút Bài nâng cao 2
        btnDocDanhBa.setOnClickListener(v -> kiemTraRoiDocDanhBa());
    }

    @Override
    protected void onResume() {
        super.onResume();
        capNhatTrangThai();
    }

    // Luồng Camera
    private void kiemTraRoiChup() {
        int trangThai = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (trangThai == PackageManager.PERMISSION_GRANTED) {
            moCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            hienGiaiThich();
        } else {
            xinQuyenCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void hienGiaiThich() {
        new AlertDialog.Builder(this)
                .setTitle("Vì sao cần quyền này?")
                .setMessage("Ứng dụng cần quyền CAMERA để chụp và hiển thị ảnh xem trước.")
                .setPositiveButton("Đồng ý", (d, w) -> xinQuyenCamera.launch(Manifest.permission.CAMERA))
                .setNegativeButton("Để sau", null)
                .show();
    }

    private void hienMoCaiDat() {
        new AlertDialog.Builder(this)
                .setTitle("Quyền đang bị chặn")
                .setMessage("Bạn đã từ chối nhiều lần. Hãy mở Cài đặt để bật quyền thủ công.")
                .setPositiveButton("Mở Cài đặt", (d, w) -> moCaiDatUngDung())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void moCamera() {
        chupAnh.launch(null);
    }

    private void moCaiDatUngDung() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    // Luồng Thông báo
    private void kiemTraRoiGuiThongBao() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            guiThongBao();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            guiThongBao();
        } else {
            xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void taoKenhThongBao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kenh = new NotificationChannel(
                    CHANNEL_ID, "Thông báo Lab A7", NotificationManager.IMPORTANCE_DEFAULT);
            kenh.setDescription("Kênh minh họa Lab A7");
            getSystemService(NotificationManager.class).createNotificationChannel(kenh);
        }
    }

    private void guiThongBao() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Xin chào từ Lab A7")
                .setContentText("Bạn vừa gửi thông báo thành công.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(this).notify(NOTI_ID, b.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Thiếu quyền gửi thông báo", e);
        }
    }

    // Luồng Bài nâng cao 2: Đọc danh bạ
    private void kiemTraRoiDocDanhBa() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            docVaHienThiDanhBa();
        } else {
            xinQuyenDanhBa.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void docVaHienThiDanhBa() {
        StringBuilder sb = new StringBuilder("5 liên hệ đầu tiên:\n");
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            int count = 0;
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            while (cursor.moveToNext() && count < 5) {
                String name = cursor.getString(nameIndex);
                sb.append("- ").append(name).append("\n");
                count++;
            }
            cursor.close();
        }
        tvTrangThai.setText(sb.toString());
    }

    // Cập nhật trạng thái hiển thị
    private void capNhatTrangThai() {
        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean thongBao = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        String s = "Quyền CAMERA: " + (camera ? "ĐÃ CẤP" : "CHƯA CẤP") +
                "\nQuyền THÔNG BÁO: " + (thongBao ? "ĐÃ CẤP" : "CHƯA CẤP");
        tvTrangThai.setText(s);
    }
}