-- =====================================================================
-- 「数智乡约」MVP 种子数据
-- 前置：已执行 schema.sql
-- 使用：docker exec -i xiangyue-mysql mysql -uroot -p200609 xiangyue < data.sql
--
-- 预置账号（密码均为 123456，admin 为 admin123）：
--   admin       (role=4 管理员)   admin / admin123
--   zhangjianguo (role=3 村长)    123456
--   lidali      (role=2 农户)    123456
--   wangcuihua  (role=2 农户)    123456
-- 演示登录后可自行 info_set 改密码/昵称/头像
-- =====================================================================

USE xiangyue;

-- ---------------------------------------------------------------------
-- 用户
-- BCrypt 哈希（$2b$12$，Spring Security BCryptPasswordEncoder 兼容）
--   admin123 -> $2b$12$gAxDaWhOEs0hw17SKiyRo.STUvi2O.xG2NeP1IAt4UdPMqE7T3i4S
--   123456   -> $2b$12$zQBiWz7IftD5ZkXe4lxqcOyTkq6I.k9s65WRqvnJNPe9ITcFM6m0i
-- ---------------------------------------------------------------------
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `email`, `avatar`, `role`, `status`) VALUES
    (1, 'admin',        '$2b$12$gAxDaWhOEs0hw17SKiyRo.STUvi2O.xG2NeP1IAt4UdPMqE7T3i4S', '13800000001', 'admin@xiangyue.com', NULL, 4, 1),
    (2, 'zhangjianguo', '$2b$12$zQBiWz7IftD5ZkXe4lxqcOyTkq6I.k9s65WRqvnJNPe9ITcFM6m0i', '13800000002', 'zj@xiangyue.com',     NULL, 3, 1),
    (3, 'lidali',       '$2b$12$zQBiWz7IftD5ZkXe4lxqcOyTkq6I.k9s65WRqvnJNPe9ITcFM6m0i', '13800000003', 'ldl@xiangyue.com',     NULL, 2, 1),
    (4, 'wangcuihua',   '$2b$12$zQBiWz7IftD5ZkXe4lxqcOyTkq6I.k9s65WRqvnJNPe9ITcFM6m0i', '13800000004', 'wch@xiangyue.com',     NULL, 2, 1);

-- ---------------------------------------------------------------------
-- 农村
-- ---------------------------------------------------------------------
INSERT INTO `village_base` (`id`, `manage_id`, `name`, `province`, `city`, `county`, `longitude`, `latitude`, `type`, `intro`, `image`, `best_time`, `activity`, `contact`) VALUES
    (1, 2, '杏花村', '安徽省', '黄山市', '歙县', 118.431200, 29.867700, 1, '始建于唐代的古村落，粉墙黛瓦，春日杏花漫山遍野，保留完整徽派建筑群，适合踏青研学。', NULL, '3-5月', '杏花节、油菜花田、徽墨制作体验', '0559-1234567'),
    (2, NULL, '桃花村', '浙江省', '湖州市', '长兴县', 119.910100, 30.887000, 3, '以桃花和农家乐闻名的民俗村，春看十里桃花，秋品水蜜桃，民宿与果园遍布。', NULL, '4-6月', '桃花节、采桃季、农家美食节', '0572-7654321');

-- ---------------------------------------------------------------------
-- 农户档案
-- business_type: 1民宿 2农产品 3文旅
-- ---------------------------------------------------------------------
INSERT INTO `farm_user` (`id`, `user_id`, `village_id`, `id_card`, `business_type`) VALUES
    (1, 2, 1, '340000197501011234', 3),
    (2, 3, 1, '340000198503152345', 1),
    (3, 4, 2, '330000199002203456', 2);

-- ---------------------------------------------------------------------
-- 景点
-- ---------------------------------------------------------------------
INSERT INTO `village_scenic` (`id`, `user_id`, `village_id`, `name`, `intro`, `image`, `price`, `type`, `has_accommodation`, `accommodation_info`, `likes`, `collections`) VALUES
    (1, 3, 1, '杏花山观景台', '登顶可俯瞰整座古村落与漫山杏花，日出时分尤为壮观。', NULL, 30, 1, 0, NULL, 128, 56),
    (2, 3, 1, '李家徽派民宿', '由百年徽商老宅改造，天井、雕花窗与徽菜一应俱全。', NULL, 0, 4, 1, '双人间380元/晚，含早餐', 96, 43),
    (3, 4, 2, '桃花溪畔露营', '溪水清澈，两岸桃林环绕，提供帐篷租赁与篝火晚会。', NULL, 50, 3, 1, '帐篷120元/顶，含地垫睡袋', 210, 98);
