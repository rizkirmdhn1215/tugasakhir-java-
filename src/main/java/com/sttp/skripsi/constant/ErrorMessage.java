package com.sttp.skripsi.constant;

public class ErrorMessage {
    // Authentication Errors
    public static final String AUTH_INVALID_CREDENTIALS = "Username atau password salah";
    public static final String AUTH_USER_NOT_FOUND = "Pengguna tidak ditemukan";
    public static final String AUTH_ACCOUNT_DISABLED = "Akun pengguna dinonaktifkan";
    public static final String AUTH_TOKEN_EXPIRED = "Sesi telah berakhir, silakan login kembali";
    public static final String AUTH_TOKEN_INVALID = "Token tidak valid";
    public static final String AUTH_UNAUTHORIZED = "Anda tidak memiliki akses untuk melakukan operasi ini";

    // User Management Errors
    public static final String USER_NOT_FOUND = "Pengguna tidak ditemukan";
    public static final String USER_ALREADY_EXISTS = "Pengguna sudah terdaftar";
    public static final String USERNAME_ALREADY_EXISTS = "Username sudah digunakan";
    public static final String EMAIL_ALREADY_EXISTS = "Email sudah terdaftar";
    public static final String USER_CREATION_FAILED = "Gagal membuat pengguna baru";
    public static final String USER_UPDATE_FAILED = "Gagal memperbarui data pengguna";
    public static final String USER_DELETE_FAILED = "Gagal menghapus pengguna";
    public static final String PASSWORD_MISMATCH = "Password tidak sesuai";
    public static final String PASSWORD_TOO_WEAK = "Password terlalu lemah, minimal 8 karakter";

    // Talent Management Errors
    public static final String TALENT_NOT_FOUND = "Data talent tidak ditemukan";
    public static final String TALENT_ALREADY_EXISTS = "Data talent sudah terdaftar";
    public static final String TALENT_CREATION_FAILED = "Gagal membuat data talent baru";
    public static final String TALENT_UPDATE_FAILED = "Gagal memperbarui data talent";
    public static final String TALENT_DELETE_FAILED = "Gagal menghapus data talent";

    // Sheet Processing Errors
    public static final String SHEET_PROCESSING_FAILED = "Gagal memproses data sheet";
    public static final String SHEET_FORMAT_INVALID = "Format sheet tidak valid";
    public static final String SHEET_DATA_INVALID = "Data dalam sheet tidak valid";
    public static final String SHEET_EMPTY = "Sheet kosong atau tidak memiliki data";

    // Database Errors
    public static final String DB_CONNECTION_ERROR = "Gagal terhubung ke database";
    public static final String DB_QUERY_ERROR = "Gagal menjalankan query database";
    public static final String DB_TRANSACTION_ERROR = "Gagal melakukan transaksi database";

    // Validation Errors
    public static final String VALIDATION_FAILED = "Data tidak valid";
    public static final String REQUIRED_FIELD_MISSING = "Field wajib diisi";
    public static final String INVALID_EMAIL_FORMAT = "Format email tidak valid";
    public static final String INVALID_DATE_FORMAT = "Format tanggal tidak valid";
    public static final String INVALID_NUMBER_FORMAT = "Format angka tidak valid";

    // File Processing Errors
    public static final String FILE_UPLOAD_FAILED = "Gagal mengunggah file";
    public static final String FILE_DELETE_FAILED = "Gagal menghapus file";
    public static final String FILE_NOT_FOUND = "File tidak ditemukan";
    public static final String FILE_SIZE_TOO_LARGE = "Ukuran file terlalu besar";
    public static final String UNSUPPORTED_FILE_TYPE = "Tipe file tidak didukung";

    // Messaging Errors
    public static final String MESSAGE_SEND_FAILED = "Gagal mengirim pesan";
    public static final String MESSAGE_PROCESSING_FAILED = "Gagal memproses pesan";
    public static final String QUEUE_CONNECTION_ERROR = "Gagal terhubung ke antrian pesan";

    // Project Errors
    public static final String PROJECT_NOT_FOUND = "Project tidak ditemukan";
    public static final String PROJECT_CREATION_FAILED = "Gagal membuat project baru";
    public static final String PROJECT_UPDATE_FAILED = "Gagal memperbarui data project";
    public static final String PROJECT_DELETE_FAILED = "Gagal menghapus project";

    // Task Errors
    public static final String TASK_NOT_FOUND = "Task tidak ditemukan";
    public static final String TASK_CREATION_FAILED = "Gagal membuat task baru";
    public static final String TASK_UPDATE_FAILED = "Gagal memperbarui data task";
    public static final String TASK_DELETE_FAILED = "Gagal menghapus task";
    public static final String TASK_PROGRESS_NOT_FOUND = "Progress task tidak ditemukan";

    // General Errors
    public static final String INTERNAL_SERVER_ERROR = "Terjadi kesalahan pada server";
    public static final String SERVICE_UNAVAILABLE = "Layanan tidak tersedia";
    public static final String REQUEST_TIMEOUT = "Permintaan timeout";
    public static final String INVALID_REQUEST = "Permintaan tidak valid";
    public static final String RESOURCE_NOT_FOUND = "Resource tidak ditemukan";
    public static final String METHOD_NOT_ALLOWED = "Metode tidak diizinkan";
} 