package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.PageResultDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.ScenicDTO;
import com.shixiaoyu.xiangyueproject.entity.po.FarmerUser;
import com.shixiaoyu.xiangyueproject.entity.po.ShopProduct;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.po.UserComment;
import com.shixiaoyu.xiangyueproject.entity.po.VillageBase;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import com.shixiaoyu.xiangyueproject.entity.vo.PageResultVO;
import com.shixiaoyu.xiangyueproject.entity.vo.ScenicVO;
import com.shixiaoyu.xiangyueproject.entity.vo.UserCommentVO;
import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.FarmerMapper;
import com.shixiaoyu.xiangyueproject.mapper.ScenicMapper;
import com.shixiaoyu.xiangyueproject.mapper.ShopProductMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserCommentMapper;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.mapper.VillageMapper;
import com.shixiaoyu.xiangyueproject.service.ScenicService;
import com.shixiaoyu.xiangyueproject.service.support.ScenicVoFiller;
import com.shixiaoyu.xiangyueproject.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 景点服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScenicServiceImpl extends ServiceImpl<ScenicMapper, VillageScenic> implements ScenicService {

    private final ScenicMapper scenicMapper;
    private final VillageMapper villageMapper;
    private final FarmerMapper farmerMapper;
    private final UserMapper userMapper;
    private final UserCommentMapper userCommentMapper;
    private final ShopProductMapper shopProductMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ScenicVoFiller scenicVoFiller;

    /**
     * 农户在本村新增景点（校验所属村一致）
     *
     * @param dto 景点信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(ScenicDTO dto) {
        if (!SecurityUtil.isFarmerOrAbove()) {
            throw new BusinessException("权限不足：仅农户/村长可新增景点");
        }
        Long currentUserId = SecurityUtil.currentUserId();
        FarmerUser fu = farmerMapper.selectOne(new LambdaQueryWrapper<FarmerUser>()
                .eq(FarmerUser::getUserId, currentUserId).last("limit 1"));
        if (fu == null || fu.getVillageId() == null) {
            throw new BusinessException("您还没有所属村落，无法新增景点");
        }
        if (!fu.getVillageId().equals(dto.getVillageId())) {
            throw new BusinessException("操作失败：只能在本村新增景点");
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setUserId(currentUserId);
        scenic.setLikes(0);
        scenic.setCollections(0);
        if (scenic.getHasAccommodation() == null) {
            scenic.setHasAccommodation(0);
        }
        if (scenicMapper.insert(scenic) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_INSERT);
        }
        // 售卖走 shop_product：新建景点时同步创建上架门票（及可选住宿核销）
        createDefaultSellProducts(scenic, dto);
    }

    /** 热门景点 TOP10（按点赞量） */
    @Override
    public List<ScenicVO> getTop10Scenic() {
        List<VillageScenic> scenics = scenicMapper.selectList(new LambdaQueryWrapper<VillageScenic>()
                .orderByDesc(VillageScenic::getLikes).last("limit 10"));
        List<ScenicVO> voList = scenics.stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        scenicVoFiller.fillTicketPrice(voList);
        return voList;
    }

    /**
     * 景点详情
     *
     * @param id 景点ID
     */
    @Override
    public ScenicVO detail(Long id) {
        VillageScenic scenic = scenicMapper.selectById(id);
        if (scenic == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        ScenicVO vo = BeanUtil.copyProperties(scenic, ScenicVO.class);
        fillScenicVillageNames(List.of(vo));
        scenicVoFiller.fillTicketPrice(List.of(vo));
        return vo;
    }

    /**
     * 景点评论树（先 Redis 缓存，未命中则查库组楼中楼后回填）
     *
     * @param id 景点ID
     */
    @Override
    public List<UserCommentVO> getScComments(Long id) {
        String key = RedisConstants.SCENIC_COMMENTS + id;
        // 1. 先读 Redis 缓存
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cached)) {
            try {
                return JSONUtil.toList(cached, UserCommentVO.class);
            } catch (Exception e) {
                log.error("景点评论缓存解析失败，key:{}", key, e);
            }
        }
        // 2. 查库 → 转 VO → 填评论人 → 组楼中楼
        List<UserComment> comments = userCommentMapper.selectList(new LambdaQueryWrapper<UserComment>()
                .eq(UserComment::getScenicId, id).orderByAsc(UserComment::getCreateTime));
        List<UserCommentVO> voList = comments.stream().map(c -> BeanUtil.copyProperties(c, UserCommentVO.class))
                .collect(Collectors.toList());
        fillCommentUsers(voList, comments);
        List<UserCommentVO> tree = buildCommentTree(voList);
        // 3. 写回 Redis
        int ttl = RandomUtil.randomInt(0, 401) + RedisConstants.USER_COMMENTS_TTL;
        try {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(tree), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("景点评论缓存写入失败，key:{}", key, e);
        }
        return tree;
    }

    /**
     * 管理端分页查询全部景点
     *
     * @param pageResultDTO 分页参数
     */
    @Override
    public PageResultVO<ScenicVO> adminList(PageResultDTO pageResultDTO) {
        SecurityUtil.requireAdmin();
        int pageNo = pageResultDTO.getPageNo() == null || pageResultDTO.getPageNo() < 1 ? 1 : pageResultDTO.getPageNo();
        int pageSize = pageResultDTO.getPageSize() == null || pageResultDTO.getPageSize() < 1 ? 10 : pageResultDTO.getPageSize();
        Page<VillageScenic> page = scenicMapper.selectPage(Page.of(pageNo, pageSize),
                new LambdaQueryWrapper<VillageScenic>().orderByDesc(VillageScenic::getCreateTime));
        List<ScenicVO> voList = page.getRecords().stream().map(s -> BeanUtil.copyProperties(s, ScenicVO.class))
                .collect(Collectors.toList());
        fillScenicVillageNames(voList);
        fillScenicCreatorNames(voList);
        scenicVoFiller.fillTicketPrice(voList);
        return new PageResultVO<>(page.getTotal(), voList);
    }

    /**
     * 管理端为指定村新增景点
     *
     * @param villageId 村落ID
     * @param dto       景点信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAdd(Long villageId, ScenicDTO dto) {
        SecurityUtil.requireAdmin();
        if (villageMapper.selectById(villageId) == null) {
            throw new BusinessException("该村落不存在");
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setVillageId(villageId);
        scenic.setUserId(SecurityUtil.currentUserId());
        scenic.setLikes(0);
        scenic.setCollections(0);
        if (scenic.getHasAccommodation() == null) {
            scenic.setHasAccommodation(0);
        }
        if (scenicMapper.insert(scenic) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_INSERT);
        }
        createDefaultSellProducts(scenic, dto);
    }

    /**
     * 新建景点后默认创建可售商品：门票核销必建；有住宿且传入 stayPrice 时再建住宿核销
     */
    private void createDefaultSellProducts(VillageScenic scenic, ScenicDTO dto) {
        BigDecimal ticketPrice = dto.getTicketPrice() != null ? dto.getTicketPrice() : BigDecimal.ZERO;
        if (ticketPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("门票价格不能为负数");
        }
        ShopProduct ticket = new ShopProduct();
        ticket.setUserId(scenic.getUserId());
        ticket.setVillageId(scenic.getVillageId());
        ticket.setScenicId(scenic.getId());
        ticket.setName(scenic.getName() + "门票");
        ticket.setIntro(scenic.getName() + "门票核销票");
        ticket.setImage(scenic.getImage());
        ticket.setPrice(ticketPrice);
        ticket.setStock(9999);
        ticket.setType(ProductTypeEnum.TICKET);
        ticket.setStatus(1);
        if (shopProductMapper.insert(ticket) != 1) {
            throw new BusinessException("创建门票商品失败");
        }

        boolean needStay = scenic.getHasAccommodation() != null && scenic.getHasAccommodation() == 1
                && dto.getStayPrice() != null;
        if (!needStay) {
            return;
        }
        if (dto.getStayPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("住宿价格不能为负数");
        }
        ShopProduct stay = new ShopProduct();
        stay.setUserId(scenic.getUserId());
        stay.setVillageId(scenic.getVillageId());
        stay.setScenicId(scenic.getId());
        stay.setName(scenic.getName() + "住宿核销");
        stay.setIntro(StrUtil.blankToDefault(scenic.getAccommodationInfo(), scenic.getName() + "住宿"));
        stay.setImage(scenic.getImage());
        stay.setPrice(dto.getStayPrice());
        stay.setStock(100);
        stay.setType(ProductTypeEnum.STAY);
        stay.setStatus(1);
        if (shopProductMapper.insert(stay) != 1) {
            throw new BusinessException("创建住宿商品失败");
        }
    }

    /**
     * 管理端修改景点
     *
     * @param id  景点ID
     * @param dto 景点信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdate(Long id, ScenicDTO dto) {
        SecurityUtil.requireAdmin();
        if (scenicMapper.selectById(id) == null) {
            throw new BusinessException(ErrorConstants.DATA_NOT_EXIST);
        }
        VillageScenic scenic = BeanUtil.copyProperties(dto, VillageScenic.class);
        scenic.setId(id);
        scenicMapper.updateById(scenic);
        syncSellProducts(scenic, dto);
    }

    /**
     * 修改景点时同步门票/住宿核销商品价格（存在则更新，不存在则创建）
     */
    private void syncSellProducts(VillageScenic scenic, ScenicDTO dto) {
        boolean hasStay = scenic.getHasAccommodation() != null && scenic.getHasAccommodation() == 1;
        if (dto.getTicketPrice() != null) {
            upsertProduct(scenic, ProductTypeEnum.TICKET, dto.getTicketPrice(), scenic.getName() + "门票");
        }
        if (hasStay && dto.getStayPrice() != null) {
            upsertProduct(scenic, ProductTypeEnum.STAY, dto.getStayPrice(), scenic.getName() + "住宿核销");
        }
    }

    private void upsertProduct(VillageScenic scenic, ProductTypeEnum type, BigDecimal price, String name) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("价格不能为负数");
        }
        ShopProduct product = shopProductMapper.selectOne(new LambdaQueryWrapper<ShopProduct>()
                .eq(ShopProduct::getScenicId, scenic.getId())
                .eq(ShopProduct::getType, type)
                .last("limit 1"));
        if (product == null) {
            product = new ShopProduct();
            product.setUserId(scenic.getUserId());
            product.setVillageId(scenic.getVillageId());
            product.setScenicId(scenic.getId());
            product.setName(name);
            product.setIntro(name);
            product.setImage(scenic.getImage());
            product.setPrice(price);
            product.setStock(type == ProductTypeEnum.TICKET ? 9999 : 100);
            product.setType(type);
            product.setStatus(1);
            shopProductMapper.insert(product);
        } else {
            ShopProduct upd = new ShopProduct();
            upd.setId(product.getId());
            upd.setPrice(price);
            shopProductMapper.updateById(upd);
        }
    }

    /**
     * 管理端删除景点
     *
     * @param id 景点ID
     */
    @Override
    public void adminDelete(Long id) {
        SecurityUtil.requireAdmin();
        if (scenicMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorConstants.ERROR_DELETE);
        }
    }

    // ===================== 私有工具 =====================

    /** 批量填充景点所属村落名（一次 IN 查询） */
    private void fillScenicVillageNames(List<ScenicVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        Set<Long> villageIds = voList.stream().map(ScenicVO::getVillageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (villageIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = villageMapper.selectByIds(villageIds).stream()
                .collect(Collectors.toMap(VillageBase::getId, VillageBase::getName, (a, b) -> a));
        voList.forEach(v -> v.setVillageName(nameMap.get(v.getVillageId())));
    }

    /** 批量填充景点创建人用户名 */
    private void fillScenicCreatorNames(List<ScenicVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        Set<Long> userIds = voList.stream().map(ScenicVO::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        voList.forEach(v -> v.setCreatorName(nameMap.get(v.getUserId())));
    }

    /** 批量填充评论人头像/昵称 */
    private void fillCommentUsers(List<UserCommentVO> voList, List<UserComment> comments) {
        if (comments.isEmpty()) {
            return;
        }
        // 1. 收集评论人 userId，批量查用户
        Set<Long> userIds = comments.stream().map(UserComment::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        // 2. 按索引回填昵称、头像
        for (int i = 0; i < voList.size(); i++) {
            User u = userMap.get(comments.get(i).getUserId());
            if (u != null) {
                voList.get(i).setUsername(u.getUsername());
                voList.get(i).setAvatar(u.getAvatar());
            }
            if (voList.get(i).getReplies() == null) {
                voList.get(i).setReplies(new ArrayList<>());
            }
        }
    }

    /** 组装楼中楼：一级 parentId 为空；子评论全部展平挂到所属根评论的 replies（B 站式） */
    private List<UserCommentVO> buildCommentTree(List<UserCommentVO> flat) {
        // 1. id → 评论节点，方便向上找根
        Map<Long, UserCommentVO> byId = flat.stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(UserCommentVO::getId, v -> v, (a, b) -> a));
        // 2. 先挑出所有一级评论（parentId 为空）
        List<UserCommentVO> roots = new ArrayList<>();
        for (UserCommentVO vo : flat) {
            if (vo.getReplies() == null) {
                vo.setReplies(new ArrayList<>());
            }
            if (vo.getParentId() == null) {
                roots.add(vo);
            }
        }
        // 3. 子评论挂到所属根评论的 replies 下
        for (UserCommentVO vo : flat) {
            if (vo.getParentId() == null) {
                continue;
            }
            UserCommentVO root = findRoot(vo, byId);
            if (root != null && !root.getId().equals(vo.getId())) {
                if (root.getReplies() == null) {
                    root.setReplies(new ArrayList<>());
                }
                root.getReplies().add(vo);
            } else {
                roots.add(vo);
            }
        }
        // 4. 根评论按时间倒序，楼内回复按时间正序
        roots.sort((a, b) -> {
            if (a.getCreateTime() == null || b.getCreateTime() == null) {
                return 0;
            }
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        for (UserCommentVO root : roots) {
            if (root.getReplies() != null) {
                root.getReplies().sort((a, b) -> {
                    if (a.getCreateTime() == null || b.getCreateTime() == null) {
                        return 0;
                    }
                    return a.getCreateTime().compareTo(b.getCreateTime());
                });
            }
        }
        return roots;
    }

    /** 沿 parentId 向上找根评论（防死循环上限 32 层） */
    private UserCommentVO findRoot(UserCommentVO vo, Map<Long, UserCommentVO> byId) {
        UserCommentVO cur = vo;
        int guard = 0;
        while (cur.getParentId() != null && guard++ < 32) {
            UserCommentVO parent = byId.get(cur.getParentId());
            if (parent == null) {
                break;
            }
            cur = parent;
        }
        return cur.getParentId() == null ? cur : null;
    }
}
