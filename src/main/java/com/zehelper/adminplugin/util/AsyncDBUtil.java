package com.zehelper.adminplugin.util;

import com.zehelper.adminplugin.AdminPlugin;
import com.zehelper.adminplugin.database.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncDBUtil {

    /** 비동기로 DB 업데이트 쿼리를 실행한다 (INSERT, UPDATE, DELETE) */
    public static CompletableFuture<Void> executeAsync(String sql, Consumer<PreparedStatement> paramSetter) {
        return CompletableFuture.runAsync(() -> {
            DatabaseManager db = AdminPlugin.getInstance().getDatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                paramSetter.accept(ps);
                ps.executeUpdate();
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("비동기 DB 실행 실패: " + e.getMessage());
                if (AdminPlugin.getInstance().getConfigManager().isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        });
    }

    /** 비동기로 DB 조회 쿼리를 실행하고 결과를 반환한다 */
    public static <T> CompletableFuture<T> queryAsync(String sql, Consumer<PreparedStatement> paramSetter, Function<ResultSet, T> resultMapper) {
        return CompletableFuture.supplyAsync(() -> {
            DatabaseManager db = AdminPlugin.getInstance().getDatabaseManager();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                paramSetter.accept(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return resultMapper.apply(rs);
                }
            } catch (Exception e) {
                AdminPlugin.getInstance().getLogger().severe("비동기 DB 조회 실패: " + e.getMessage());
                if (AdminPlugin.getInstance().getConfigManager().isDebugEnabled()) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    /** 비동기 작업 완료 후 메인 스레드에서 콜백을 실행한다 */
    public static <T> void runOnMain(CompletableFuture<T> future, Consumer<T> callback) {
        future.thenAccept(result ->
            Bukkit.getScheduler().runTask(AdminPlugin.getInstance(), () -> callback.accept(result))
        );
    }
}
