package cn.easykits.oa.modules.office.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import cn.easykits.oa.common.api.PageResult;
import cn.easykits.oa.common.api.ResultCode;
import cn.easykits.oa.common.exception.BusinessException;
import cn.easykits.oa.entity.OfficeAsset;
import cn.easykits.oa.entity.OfficeAssetRecord;
import cn.easykits.oa.entity.SysUser;
import cn.easykits.oa.mapper.OfficeAssetMapper;
import cn.easykits.oa.mapper.OfficeAssetRecordMapper;
import cn.easykits.oa.mapper.SysUserMapper;
import cn.easykits.oa.modules.office.dto.AssetActionForm;
import cn.easykits.oa.modules.office.dto.AssetForm;
import cn.easykits.oa.modules.office.vo.AssetRecordVO;
import cn.easykits.oa.modules.office.vo.AssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资产管理：台账维护 + 领用/归还/转移/报废/丢失流转，全程落 office_asset_record。
 */
@Service
@RequiredArgsConstructor
public class AssetService extends ServiceImpl<OfficeAssetMapper, OfficeAsset> {

    public static final Integer STATUS_IN_STOCK = 0;
    public static final Integer STATUS_IN_USE = 1;
    public static final Integer STATUS_SCRAPPED = 3;
    public static final Integer STATUS_LOST = 4;

    public static final String ACTION_ASSIGN = "assign";
    public static final String ACTION_RETURN = "return";
    public static final String ACTION_TRANSFER = "transfer";
    public static final String ACTION_SCRAP = "scrap";
    public static final String ACTION_LOST = "lost";

    private final OfficeAssetRecordMapper recordMapper;
    private final SysUserMapper sysUserMapper;

    /* ==================== 台账 ==================== */

    public PageResult<AssetVO> pageQuery(long pageNum, long pageSize,
                                         String keyword, String category, Integer status) {
        Page<OfficeAsset> page = page(Page.of(pageNum, pageSize),
                Wrappers.<OfficeAsset>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(OfficeAsset::getAssetNo, keyword)
                                .or().like(OfficeAsset::getName, keyword)
                                .or().like(OfficeAsset::getBrandModel, keyword))
                        .eq(StringUtils.hasText(category), OfficeAsset::getCategory, category)
                        .eq(status != null, OfficeAsset::getStatus, status)
                        .orderByDesc(OfficeAsset::getId));
        return PageResult.of(page, this::toVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public OfficeAsset create(AssetForm form) {
        checkAssetNoUnique(form.assetNo(), null);
        OfficeAsset asset = new OfficeAsset();
        applyForm(asset, form);
        asset.setStatus(STATUS_IN_STOCK);
        save(asset);
        return asset;
    }

    @Transactional(rollbackFor = Exception.class)
    public OfficeAsset update(Long id, AssetForm form) {
        OfficeAsset asset = requireById(id);
        if (!Objects.equals(asset.getStatus(), STATUS_SCRAPPED)) {
            checkAssetNoUnique(form.assetNo(), id);
        } else {
            throw new BusinessException("已报废资产不可编辑");
        }
        applyForm(asset, form);
        if (!updateById(asset)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return asset;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OfficeAsset asset = requireById(id);
        if (Objects.equals(asset.getStatus(), STATUS_IN_USE)) {
            throw new BusinessException("资产在用中，请先归还再删除");
        }
        removeById(id);
        recordMapper.delete(Wrappers.<OfficeAssetRecord>lambdaQuery()
                .eq(OfficeAssetRecord::getAssetId, id));
    }

    /* ==================== 流转 ==================== */

    /** 领用：在库 → 在用 */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, AssetActionForm form) {
        OfficeAsset asset = requireById(id);
        if (!Objects.equals(asset.getStatus(), STATUS_IN_STOCK)) {
            throw new BusinessException("仅「在库」资产可领用");
        }
        requireUser(form.userId());
        asset.setStatus(STATUS_IN_USE);
        asset.setHolderUserId(form.userId());
        updateAndLog(asset, ACTION_ASSIGN, form.userId(), form.remark());
    }

    /** 归还：在用 → 在库（仅当前使用人或资产管理员可操作） */
    @Transactional(rollbackFor = Exception.class)
    public void giveBack(Long id, String remark) {
        OfficeAsset asset = requireById(id);
        if (!Objects.equals(asset.getStatus(), STATUS_IN_USE)) {
            throw new BusinessException("仅「在用」资产可归还");
        }
        Long holder = asset.getHolderUserId();
        asset.setStatus(STATUS_IN_STOCK);
        asset.setHolderUserId(null);
        updateAndLog(asset, ACTION_RETURN, holder, remark);
    }

    /** 转移：在用 → 在用（换使用人） */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long id, AssetActionForm form) {
        OfficeAsset asset = requireById(id);
        if (!Objects.equals(asset.getStatus(), STATUS_IN_USE)) {
            throw new BusinessException("仅「在用」资产可转移");
        }
        requireUser(form.userId());
        if (Objects.equals(asset.getHolderUserId(), form.userId())) {
            throw new BusinessException("新使用人与当前使用人相同");
        }
        Long prevHolder = asset.getHolderUserId();
        asset.setHolderUserId(form.userId());
        updateAndLog(asset, ACTION_TRANSFER, form.userId(),
                StringUtils.hasText(form.remark())
                        ? form.remark()
                        : "由用户 " + prevHolder + " 转移");
    }

    /** 报废：需不在用（在用先归还） */
    @Transactional(rollbackFor = Exception.class)
    public void scrap(Long id, String remark) {
        OfficeAsset asset = requireById(id);
        if (Objects.equals(asset.getStatus(), STATUS_IN_USE)) {
            throw new BusinessException("资产在用中，请先归还再报废");
        }
        if (Objects.equals(asset.getStatus(), STATUS_SCRAPPED)) {
            return;
        }
        asset.setStatus(STATUS_SCRAPPED);
        updateAndLog(asset, ACTION_SCRAP, null, remark);
    }

    /** 丢失登记 */
    @Transactional(rollbackFor = Exception.class)
    public void markLost(Long id, String remark) {
        OfficeAsset asset = requireById(id);
        if (Objects.equals(asset.getStatus(), STATUS_SCRAPPED)
                || Objects.equals(asset.getStatus(), STATUS_LOST)) {
            return;
        }
        Long holder = asset.getHolderUserId();
        asset.setStatus(STATUS_LOST);
        asset.setHolderUserId(null);
        updateAndLog(asset, ACTION_LOST, holder, remark);
    }

    /** 流转记录（按时间倒序） */
    public List<AssetRecordVO> records(Long assetId) {
        requireById(assetId);
        List<OfficeAssetRecord> records = recordMapper.selectList(
                Wrappers.<OfficeAssetRecord>lambdaQuery()
                        .eq(OfficeAssetRecord::getAssetId, assetId)
                        .orderByDesc(OfficeAssetRecord::getId));
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = records.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getTargetUserId(), r.getCreatedBy()))
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUser> users = sysUserMapper.selectList(
                        Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds)).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return records.stream().map(r -> {
            SysUser target = r.getTargetUserId() == null ? null : users.get(r.getTargetUserId());
            SysUser operator = r.getCreatedBy() == null ? null : users.get(r.getCreatedBy());
            return new AssetRecordVO(r.getId(), r.getAction(), r.getAssetStatus(),
                    r.getTargetUserId(),
                    target == null ? (r.getTargetUserId() == null ? null : String.valueOf(r.getTargetUserId()))
                            : target.getNickname(),
                    operator == null ? null : operator.getNickname(),
                    r.getRemark(), r.getCreatedAt());
        }).toList();
    }

    /* ==================== 私有 ==================== */

    private OfficeAsset requireById(Long id) {
        OfficeAsset asset = getById(id);
        if (asset == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "资产不存在");
        }
        return asset;
    }

    private void requireUser(Long userId) {
        if (userId == null || sysUserMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "使用人不存在");
        }
    }

    private void checkAssetNoUnique(String assetNo, Long excludeId) {
        Long count = count(Wrappers.<OfficeAsset>lambdaQuery()
                .eq(OfficeAsset::getAssetNo, assetNo)
                .ne(excludeId != null, OfficeAsset::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("资产编号已存在：" + assetNo);
        }
    }

    private void applyForm(OfficeAsset asset, AssetForm form) {
        asset.setAssetNo(form.assetNo());
        asset.setName(form.name());
        asset.setCategory(form.category());
        asset.setBrandModel(form.brandModel());
        asset.setPurchaseDate(form.purchaseDate());
        asset.setPrice(form.price());
        asset.setLocation(form.location());
        asset.setRemark(form.remark());
    }

    private void updateAndLog(OfficeAsset asset, String action, Long targetUserId, String remark) {
        if (!updateById(asset)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        OfficeAssetRecord record = new OfficeAssetRecord();
        record.setAssetId(asset.getId());
        record.setAction(action);
        record.setTargetUserId(targetUserId);
        record.setAssetStatus(asset.getStatus());
        record.setRemark(remark);
        recordMapper.insert(record);
    }

    private AssetVO toVO(OfficeAsset asset) {
        String holderName = asset.getHolderUserId() == null ? null
                : java.util.Optional.ofNullable(sysUserMapper.selectById(asset.getHolderUserId()))
                .map(SysUser::getNickname).orElse(null);
        return new AssetVO(asset.getId(), asset.getAssetNo(), asset.getName(),
                asset.getCategory(), asset.getBrandModel(), asset.getPurchaseDate(),
                asset.getPrice(), asset.getLocation(), asset.getStatus(),
                asset.getHolderUserId(), holderName, asset.getRemark(), asset.getCreatedAt());
    }
}
